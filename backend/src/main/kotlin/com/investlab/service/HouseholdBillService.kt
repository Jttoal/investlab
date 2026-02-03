package com.investlab.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.investlab.exception.ForbiddenException
import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.*
import com.investlab.repository.StatementFileRepository
import com.investlab.repository.StatementTransactionRepository
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class HouseholdBillService(
    private val statementFileRepository: StatementFileRepository,
    private val statementTransactionRepository: StatementTransactionRepository,
    private val settingService: SettingService,
    private val objectMapper: ObjectMapper
) {

    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}\\s+")
    private val currencyPattern = Regex("^([A-Z]{3})\\s+")
    private val amountPattern = Regex("^(-?[\\d,]+\\.\\d{2})\\s+")
    private val balancePattern = Regex("^([\\d,]+\\.\\d{2})\\s+")
    private val pageFooterPattern = Regex("^\\d+/\\d+\\s*$")
    private val headerKeywords = listOf("记账日期", "货币", "交易金额", "联机余额", "交易摘要", "对手信息")
    private val englishHeaderKeywords = listOf("Date", "Currency", "Amount", "Balance", "Transaction", "Counter Party")

    private val defaultSummaryKeywords = setOf(
        "受托理财申购",
        "受托理财赎回",
        "基金定期定额申购",
        "基金申购",
        "申购",
        "基金赎回",
        "朝朝宝转入",
        "朝朝宝自动转入",
        "朝朝宝转出",
        "基金认购",
        "银证转账(第三方存管)",
        "受托理财分红"
    )
    private val defaultCounterpartyKeywords = setOf(
        "盈米基金",
        "蚂蚁基金",
        "广发基金",
        "景顺长城基金",
        "基金销售"
    )

    private val uploadDir: Path = Paths.get("uploads/household-bills")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    data class ParsedRow(
        val txnDate: LocalDate,
        val currency: String,
        val amount: BigDecimal,
        val balance: BigDecimal?,
        val txnType: String,
        val counterparty: String,
        val rawLine: String
    )

    data class ParseResult(
        val accountName: String?,
        val rows: List<ParsedRow>
    )

    data class InvestKeywordConfig(
        val billing_summary_keyword: List<String> = emptyList(),
        val counter_party_keyword: List<String> = emptyList()
    )

    @Transactional
    fun upload(file: MultipartFile, userId: Long = 0L): StatementFileResponse {
        require(!file.isEmpty) { "文件不能为空" }

        Files.createDirectories(uploadDir)
        val originalName = file.originalFilename ?: "statement.pdf"
        val storedPath = uploadDir.resolve("${System.currentTimeMillis()}_${originalName}")
        file.inputStream.use { input ->
            Files.copy(input, storedPath)
        }
        val md5 = md5Hex(Files.readAllBytes(storedPath))

        var statementFile = statementFileRepository.save(
            StatementFile(
                userId = userId,
                fileName = originalName,
                fileMd5 = md5,
                filePath = storedPath.toAbsolutePath().toString(),
                status = StatementFileStatus.processing
            )
        )

        try {
            val parseResult = parsePdf(storedPath)
            val parsedRows = parseResult.rows
            val keywords = loadKeywords()
            val rules = getRules(userId)
            val seqCounter = mutableMapOf<String, Int>()
            var dedupCount = 0
            val toInsert = mutableListOf<StatementTransaction>()

            parsedRows.forEach { row ->
                val seqKey = "${row.txnDate}_${row.amount.setScale(2)}"
                val seq = (seqCounter[seqKey] ?: 0) + 1
                seqCounter[seqKey] = seq
                val dedupKey = "${row.txnDate.format(dateFormatter)}_${row.amount.setScale(2)}_${seq}"

                if (statementTransactionRepository.existsByDedupKey(dedupKey)) {
                    dedupCount++
                    return@forEach
                }

                // 应用替换规则 - 匹配到第一条规则后停止
                var processedSummary = row.txnType
                for (rule in rules.replaceRules) {
                    val shouldReplace = when (rule.matchType) {
                        "counterparty" -> row.counterparty.contains(rule.pattern, ignoreCase = true)
                        "both" -> {
                            // 同时匹配：交易摘要包含pattern，且对手信息包含counterpartyPattern
                            val summaryMatch = row.txnType.contains(rule.pattern, ignoreCase = true)
                            val counterpartyMatch = rule.counterpartyPattern?.let {
                                row.counterparty.contains(it, ignoreCase = true)
                            } ?: false
                            summaryMatch && counterpartyMatch
                        }
                        else -> row.txnType.contains(rule.pattern, ignoreCase = true)
                    }
                    if (shouldReplace) {
                        processedSummary = rule.replacement
                        break // 匹配到第一条规则后立即退出
                    }
                }

                val (category, direction) = classify(row, keywords)
                toInsert.add(
                    StatementTransaction(
                        userId = userId,
                        fileId = statementFile.id!!,
                        txnDate = row.txnDate,
                        currency = row.currency,
                        amount = row.amount,
                        balance = row.balance,
                        txnTypeRaw = processedSummary,
                        txnTypeRawOriginal = row.txnType,  // 保存原始交易摘要
                        counterparty = row.counterparty,
                        accountName = parseResult.accountName,
                        category = category,
                        direction = direction,
                        dedupKey = dedupKey,
                        hashRaw = row.rawLine
                    )
                )
            }

            if (toInsert.isNotEmpty()) {
                statementTransactionRepository.saveAll(toInsert)
            }

            statementFile.totalRows = parsedRows.size
            statementFile.insertedRows = toInsert.size
            statementFile.dedupRows = dedupCount
            statementFile.failedRows = parsedRows.size - toInsert.size - dedupCount
            statementFile.accountName = parseResult.accountName
            statementFile.status = StatementFileStatus.success
            statementFile.finishedAt = LocalDateTime.now()
            statementFile = statementFileRepository.save(statementFile)
            return StatementFileResponse.from(statementFile)
        } catch (ex: Exception) {
            statementFile.status = StatementFileStatus.failed
            statementFile.errorMsg = ex.message
            statementFile.finishedAt = LocalDateTime.now()
            statementFileRepository.save(statementFile)
            throw ex
        }
    }

    fun getFile(id: Long): StatementFileResponse {
        val entity = statementFileRepository.findById(id)
            .orElseThrow { IllegalArgumentException("文件不存在: $id") }
        return StatementFileResponse.from(entity)
    }

    fun listTransactions(
        startDate: LocalDate?,
        endDate: LocalDate?,
        category: TransactionCategory?,
        direction: TransactionDirection?,
        keyword: String?,
        accountName: String?,
        counterparty: String?,
        amountDirection: String?
    ): List<StatementTransactionResponse> {
        return statementTransactionRepository.search(startDate, endDate, category, direction, keyword, accountName, counterparty, amountDirection)
            .map { StatementTransactionResponse.from(it) }
    }

    fun summarize(
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<StatementSummaryResponse> {
        val results = statementTransactionRepository.summarizeByMonth(startDate, endDate)
        return results.mapNotNull { row ->
            try {
                val month = row[0] as String
                val category = when (row[1]) {
                    is TransactionCategory -> row[1] as TransactionCategory
                    is String -> TransactionCategory.valueOf((row[1] as String).lowercase())
                    else -> return@mapNotNull null
                }
                val direction = when (row[2]) {
                    is TransactionDirection -> row[2] as TransactionDirection
                    is String -> TransactionDirection.valueOf((row[2] as String).lowercase())
                    else -> return@mapNotNull null
                }
                val amount = when (val amt = row[3]) {
                    is BigDecimal -> amt
                    is Number -> BigDecimal.valueOf(amt.toDouble())
                    is String -> BigDecimal(amt)
                    else -> BigDecimal.ZERO
                }
                StatementSummaryResponse(month, category, direction, amount)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parsePdf(path: Path): ParseResult {
        val rows = mutableListOf<ParsedRow>()
        var accountName: String? = null
        // 匹配 "户  名：xxx"，其中空格可能是普通空格或不间断空格(nbsp, \u00A0)
        val accountPattern = Regex("户[\\s\u00A0]+名[\\s\u00A0]*[：:][\\s\u00A0]*([^\\s\u00A0\\n]+)")

        Loader.loadPDF(path.toFile()).use { document ->
            val stripper = PDFTextStripper()
            for (page in 1..document.numberOfPages) {
                stripper.startPage = page
                stripper.endPage = page
                val text = stripper.getText(document) ?: continue

                // 第一页提取户名
                if (page == 1 && accountName.isNullOrBlank()) {
                    val match = accountPattern.find(text)
                    accountName = match?.groupValues?.getOrNull(1)
                }

                val lines = text.split("\n")
                var i = 0
                while (i < lines.size) {
                    val line = lines[i].trim()

                    // 跳过空行、表头、页脚
                    if (line.isBlank() || isHeaderLine(line) || isEnglishHeaderLine(line) || isPageFooter(line)) {
                        i++
                        continue
                    }

                    // 检查是否是数据行
                    if (isDataLine(line)) {
                        val parsed = parseDataLine(line)
                        if (parsed != null) {
                            var counterparty = parsed.counterparty

                            // 如果对手信息为空，尝试从后续2行拼接
                            if (counterparty.isBlank() && i + 2 < lines.size) {
                                val next1 = lines[i + 1].trim()
                                val next2 = lines[i + 2].trim()

                                // 检查后续2行是否都不是数据行、表头、页脚
                                val next1IsValid = next1.isNotBlank() && !isDataLine(next1) &&
                                                   !isHeaderLine(next1) && !isEnglishHeaderLine(next1) && !isPageFooter(next1)
                                val next2IsValid = next2.isNotBlank() && !isDataLine(next2) &&
                                                   !isHeaderLine(next2) && !isEnglishHeaderLine(next2) && !isPageFooter(next2)

                                if (next1IsValid && next2IsValid) {
                                    // 拼接2行作为对手信息
                                    counterparty = "$next1$next2"
                                    i += 3  // 跳过当前行和后续2行
                                } else if (next1IsValid) {
                                    // 只有第1行有效
                                    counterparty = next1
                                    i += 2
                                } else {
                                    i += 1
                                }
                            } else {
                                i += 1
                            }

                            rows.add(parsed.copy(counterparty = counterparty))
                        } else {
                            i += 1
                        }
                    } else {
                        i++
                    }
                }
            }
        }
        return ParseResult(accountName, rows)
    }

    private fun isHeaderLine(line: String): Boolean {
        val matches = headerKeywords.count { line.contains(it) }
        return matches >= 4
    }

    private fun isEnglishHeaderLine(line: String): Boolean {
        // 英文表头检测：需要包含至少4个关键词，且不能是数据行
        if (isDataLine(line)) return false
        val matches = englishHeaderKeywords.count { line.contains(it) }
        return matches >= 4
    }

    private fun isPageFooter(line: String): Boolean = pageFooterPattern.matches(line.trim())

    private fun isDataLine(line: String): Boolean = datePattern.containsMatchIn(line.trim())

    private fun parseDataLine(line: String): ParsedRow? {
        val trimmed = line.trim()
        val dateMatch = datePattern.find(trimmed) ?: return null
        val dateStr = dateMatch.value.trim()
        val txnDate = LocalDate.parse(dateStr)
        var rest = trimmed.removePrefix(dateMatch.value).trim()

        val currencyMatch = currencyPattern.find(rest) ?: return null
        val currency = currencyMatch.groupValues[1]
        rest = rest.removeRange(currencyMatch.range).trim()

        val amountMatch = amountPattern.find(rest) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = BigDecimal(amountStr)
        rest = rest.removeRange(amountMatch.range).trim()

        val balanceMatch = balancePattern.find(rest) ?: return null
        val balanceStr = balanceMatch.groupValues[1].replace(",", "")
        val balance = BigDecimal(balanceStr)
        rest = rest.removeRange(balanceMatch.range).trim()

        val parts = rest.split(Regex("\\s+"), limit = 2)
        val txnType = parts.getOrNull(0) ?: ""
        val counterparty = parts.getOrNull(1) ?: ""

        return ParsedRow(
            txnDate = txnDate,
            currency = currency,
            amount = amount,
            balance = balance,
            txnType = txnType,
            counterparty = counterparty,
            rawLine = trimmed
        )
    }

    private fun classify(row: ParsedRow, keywords: InvestKeywordConfig): Pair<TransactionCategory, TransactionDirection> {
        val summaryLower = row.txnType.lowercase()
        val counterLower = row.counterparty.lowercase()
        val summaryKeywords = if (keywords.billing_summary_keyword.isNotEmpty()) {
            keywords.billing_summary_keyword
        } else {
            defaultSummaryKeywords
        }
        val counterKeywords = if (keywords.counter_party_keyword.isNotEmpty()) {
            keywords.counter_party_keyword
        } else {
            defaultCounterpartyKeywords
        }

        val isInvestment = summaryKeywords.any { summaryLower.contains(it.lowercase()) } ||
            counterLower.contains("基金销售") ||
            counterKeywords.any { counterLower.contains(it.lowercase()) }

        return if (isInvestment) {
            if (row.amount < BigDecimal.ZERO) {
                TransactionCategory.investment to TransactionDirection.buy
            } else {
                TransactionCategory.investment to TransactionDirection.redeem
            }
        } else {
            if (row.amount < BigDecimal.ZERO) {
                TransactionCategory.ordinary to TransactionDirection.expense
            } else {
                TransactionCategory.ordinary to TransactionDirection.income
            }
        }
    }

    private fun loadKeywords(): InvestKeywordConfig {
        val config = settingService.getByKey("household_bills.invest_category")?.value ?: return InvestKeywordConfig()
        return try {
            objectMapper.readValue(config, InvestKeywordConfig::class.java)
        } catch (_: Exception) {
            InvestKeywordConfig()
        }
    }

    private fun md5Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    // 新增统计方法
    fun getMonthlySummary(
        month: String,  // YYYY-MM
        category: String,  // ordinary 或 investment
        accountNames: List<String>?
    ): MonthlySummaryResponse {
        val startDate = "$month-01"
        val endDate = LocalDate.parse(startDate).plusMonths(1).toString()

        val directions = if (category == "ordinary") {
            listOf("expense", "income")
        } else {
            listOf("buy", "redeem")
        }

        val expenseDirections = if (category == "ordinary") listOf("expense") else listOf("buy")
        val incomeDirections = if (category == "ordinary") listOf("income") else listOf("redeem")

        // 获取支出明细
        val expenseDetails = statementTransactionRepository.summarizeByTypeRaw(
            category, expenseDirections, startDate, endDate, accountNames
        ).map {
            val amountValue = when (val amt = it[1]) {
                is Double -> BigDecimal.valueOf(amt)
                is Float -> BigDecimal.valueOf(amt.toDouble())
                is Number -> BigDecimal(amt.toString())
                else -> BigDecimal(amt.toString())
            }
            SummaryItemResponse(
                summary = it[0] as String,
                amount = amountValue
            )
        }

        // 获取收入明细
        val incomeDetails = statementTransactionRepository.summarizeByTypeRaw(
            category, incomeDirections, startDate, endDate, accountNames
        ).map {
            val amountValue = when (val amt = it[1]) {
                is Double -> BigDecimal.valueOf(amt)
                is Float -> BigDecimal.valueOf(amt.toDouble())
                is Number -> BigDecimal(amt.toString())
                else -> BigDecimal(amt.toString())
            }
            SummaryItemResponse(
                summary = it[0] as String,
                amount = amountValue
            )
        }

        // 计算总览
        val totalExpense = expenseDetails.sumOf { it.amount }
        val totalIncome = incomeDetails.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        return MonthlySummaryResponse(
            month = month,
            category = category,
            overview = OverviewResponse(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance
            ),
            expenseDetails = expenseDetails,
            incomeDetails = incomeDetails
        )
    }

    fun getYearlySummary(
        year: String,  // YYYY
        category: String,
        accountNames: List<String>?
    ): MonthlySummaryResponse {
        val startDate = "$year-01-01"
        val endDate = "${year.toInt() + 1}-01-01"

        val directions = if (category == "ordinary") {
            listOf("expense", "income")
        } else {
            listOf("buy", "redeem")
        }

        val expenseDirections = if (category == "ordinary") listOf("expense") else listOf("buy")
        val incomeDirections = if (category == "ordinary") listOf("income") else listOf("redeem")

        val expenseDetails = statementTransactionRepository.summarizeByTypeRaw(
            category, expenseDirections, startDate, endDate, accountNames
        ).map {
            val amountValue = when (val amt = it[1]) {
                is Double -> BigDecimal.valueOf(amt)
                is Float -> BigDecimal.valueOf(amt.toDouble())
                is Number -> BigDecimal(amt.toString())
                else -> BigDecimal(amt.toString())
            }
            SummaryItemResponse(
                summary = it[0] as String,
                amount = amountValue
            )
        }

        val incomeDetails = statementTransactionRepository.summarizeByTypeRaw(
            category, incomeDirections, startDate, endDate, accountNames
        ).map {
            val amountValue = when (val amt = it[1]) {
                is Double -> BigDecimal.valueOf(amt)
                is Float -> BigDecimal.valueOf(amt.toDouble())
                is Number -> BigDecimal(amt.toString())
                else -> BigDecimal(amt.toString())
            }
            SummaryItemResponse(
                summary = it[0] as String,
                amount = amountValue
            )
        }

        val totalExpense = expenseDetails.sumOf { it.amount }
        val totalIncome = incomeDetails.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        return MonthlySummaryResponse(
            month = year,  // 这里用year字段表示年份
            category = category,
            overview = OverviewResponse(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance
            ),
            expenseDetails = expenseDetails,
            incomeDetails = incomeDetails
        )
    }

    fun getMonthlyTrend(
        year: String,
        category: String,
        accountNames: List<String>?
    ): TrendResponse {
        val startDate = "$year-01-01"
        val endDate = "${year.toInt() + 1}-01-01"

        val incomeDirections = if (category == "ordinary") listOf("income") else listOf("redeem")
        val expenseDirections = if (category == "ordinary") listOf("expense") else listOf("buy")

        val data = statementTransactionRepository.getTrendData(
            format = "%Y-%m",
            category = category,
            incomeDirections = incomeDirections,
            expenseDirections = expenseDirections,
            startDate = startDate,
            endDate = endDate,
            accountNames = accountNames
        ).map {
            TrendDataPoint(
                period = it[0] as String,
                income = BigDecimal(it[1].toString()),
                expense = BigDecimal(it[2].toString()),
                balance = BigDecimal(it[3].toString())
            )
        }

        return TrendResponse(category = category, data = data)
    }

    fun getYearlyTrend(
        category: String,
        accountNames: List<String>?
    ): TrendResponse {
        // 获取数据库中的最早和最晚日期
        val startDate = "2000-01-01"  // 足够早的日期
        val endDate = "2099-12-31"    // 足够晚的日期

        val incomeDirections = if (category == "ordinary") listOf("income") else listOf("redeem")
        val expenseDirections = if (category == "ordinary") listOf("expense") else listOf("buy")

        val data = statementTransactionRepository.getTrendData(
            format = "%Y",
            category = category,
            incomeDirections = incomeDirections,
            expenseDirections = expenseDirections,
            startDate = startDate,
            endDate = endDate,
            accountNames = accountNames
        ).map {
            TrendDataPoint(
                period = it[0] as String,
                income = BigDecimal(it[1].toString()),
                expense = BigDecimal(it[2].toString()),
                balance = BigDecimal(it[3].toString())
            )
        }

        return TrendResponse(category = category, data = data)
    }

    fun updateTransactionSummary(userId: Long, transactionId: Long, newSummary: String): StatementTransactionResponse {
        val transaction = statementTransactionRepository.findById(transactionId)
            .orElseThrow { ResourceNotFoundException("交易记录不存在") }

        if (transaction.userId != userId) {
            throw ForbiddenException("无权限修改此交易记录")
        }

        transaction.txnTypeRaw = newSummary
        val saved = statementTransactionRepository.save(transaction)
        return StatementTransactionResponse.from(saved)
    }

    fun getAccountNames(userId: Long): List<String> {
        return statementTransactionRepository.findAll()
            .filter { it.userId == userId && !it.accountName.isNullOrBlank() }
            .map { it.accountName!! }
            .distinct()
            .sorted()
    }

    // 获取导入规则
    fun getRules(userId: Long): ImportRulesResponse {
        val setting = settingService.getByKey("household_bills.invest_category")
        if (setting == null) {
            return ImportRulesResponse(
                summaryKeywords = emptyList(),
                counterpartyKeywords = emptyList(),
                replaceRules = emptyList()
            )
        }

        return try {
            val config = objectMapper.readValue(setting.value, Map::class.java) as Map<*, *>
            ImportRulesResponse(
                summaryKeywords = (config["billing_summary_keyword"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                counterpartyKeywords = (config["counter_party_keyword"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                replaceRules = (config["replace_rules"] as? List<*>)?.mapNotNull { rule ->
                    (rule as? Map<*, *>)?.let {
                        ReplaceRule(
                            pattern = it["pattern"] as? String ?: "",
                            replacement = it["replacement"] as? String ?: "",
                            matchType = it["matchType"] as? String ?: "summary",
                            counterpartyPattern = it["counterpartyPattern"] as? String
                        )
                    }
                } ?: emptyList()
            )
        } catch (e: Exception) {
            ImportRulesResponse(
                summaryKeywords = emptyList(),
                counterpartyKeywords = emptyList(),
                replaceRules = emptyList()
            )
        }
    }

    // 保存导入规则
    fun saveRules(userId: Long, request: ImportRulesRequest): ImportRulesResponse {
        val config = mapOf(
            "billing_summary_keyword" to request.summaryKeywords,
            "counter_party_keyword" to request.counterpartyKeywords,
            "replace_rules" to request.replaceRules.map {
                val ruleMap = mutableMapOf(
                    "pattern" to it.pattern,
                    "replacement" to it.replacement,
                    "matchType" to it.matchType
                )
                if (it.counterpartyPattern != null) {
                    ruleMap["counterpartyPattern"] = it.counterpartyPattern
                }
                ruleMap
            }
        )
        val jsonValue = objectMapper.writeValueAsString(config)

        val existing = settingService.getByKey("household_bills.invest_category")
        if (existing != null) {
            settingService.update(existing.id!!, SettingRequest(
                key = "household_bills.invest_category",
                value = jsonValue,
                description = "家庭账单投资分类规则"
            ))
        } else {
            settingService.create(SettingRequest(
                key = "household_bills.invest_category",
                value = jsonValue,
                description = "家庭账单投资分类规则"
            ))
        }

        return request.let {
            ImportRulesResponse(
                summaryKeywords = it.summaryKeywords,
                counterpartyKeywords = it.counterpartyKeywords,
                replaceRules = it.replaceRules
            )
        }
    }

    // 预览规则重跑
    fun previewRerun(userId: Long): List<RerunChangeResponse> {
        val rules = getRules(userId)
        val transactions = statementTransactionRepository.findAll().filter { it.userId == userId }
        val changes = mutableListOf<RerunChangeResponse>()

        transactions.forEach { txn ->
            var categoryChanged = false
            var oldCategory: String? = null
            var newCategory: String? = null
            var summaryChanged = false
            var oldSummary: String? = null
            var newSummary: String? = null

            // 检查分类是否需要变更
            val summaryLower = (txn.txnTypeRawOriginal ?: txn.txnTypeRaw ?: "").lowercase()
            val counterLower = (txn.counterparty ?: "").lowercase()

            val shouldBeInvestment = rules.summaryKeywords.any { summaryLower.contains(it.lowercase()) } ||
                                    rules.counterpartyKeywords.any { counterLower.contains(it.lowercase()) }

            val currentIsInvestment = txn.category == TransactionCategory.investment

            if (shouldBeInvestment != currentIsInvestment) {
                categoryChanged = true
                oldCategory = if (currentIsInvestment) "投资" else "普通"
                newCategory = if (shouldBeInvestment) "投资" else "普通"
            }

            // 检查摘要是否需要替换 - 匹配到第一条规则后停止
            val originalSummary = txn.txnTypeRawOriginal ?: txn.txnTypeRaw ?: ""
            val counterparty = txn.counterparty ?: ""
            var matchedRule: ReplaceRule? = null;
            for (rule in rules.replaceRules) {
                val ruleMatched = when (rule.matchType) {
                    "counterparty" -> counterparty.contains(rule.pattern, ignoreCase = true)
                    "both" -> {
                        val summaryMatch = originalSummary.contains(rule.pattern, ignoreCase = true)
                        val counterpartyMatch = rule.counterpartyPattern?.let {
                            counterparty.contains(it, ignoreCase = true)
                        } ?: false
                        summaryMatch && counterpartyMatch
                    }
                    else -> originalSummary.contains(rule.pattern, ignoreCase = true)
                }

                if (ruleMatched ) {
                    matchedRule = rule;
                    break // 匹配到第一条规则后立即退出
                }
            }
            if (txn.txnTypeRaw != matchedRule?.replacement) {
                summaryChanged = true
                oldSummary = txn.txnTypeRaw
                newSummary = matchedRule?.replacement
            }
            if (categoryChanged || summaryChanged) {
                changes.add(RerunChangeResponse(
                    id = txn.id!!,
                    txnDate = txn.txnDate.toString(),
                    amount = txn.amount.toString(),
                    counterparty = txn.counterparty,
                    originalSummary = originalSummary,
                    currentSummary = txn.txnTypeRaw ?: "",
                    categoryChange = categoryChanged,
                    oldCategory = oldCategory,
                    newCategory = newCategory,
                    summaryChange = summaryChanged,
                    oldSummary = oldSummary,
                    newSummary = newSummary
                ))
            }
        }

        return changes
    }

    // 执行规则重跑
    @Transactional
    fun executeRerun(userId: Long, changeIds: List<Long>): Int {
        val rules = getRules(userId)
        val transactions = statementTransactionRepository.findAllById(changeIds)
            .filter { it.userId == userId }

        transactions.forEach { txn ->
            // 重新分类
            val summaryLower = (txn.txnTypeRawOriginal ?: txn.txnTypeRaw ?: "").lowercase()
            val counterLower = (txn.counterparty ?: "").lowercase()

            val shouldBeInvestment = rules.summaryKeywords.any { summaryLower.contains(it.lowercase()) } ||
                                    rules.counterpartyKeywords.any { counterLower.contains(it.lowercase()) }

            if (shouldBeInvestment) {
                txn.category = TransactionCategory.investment
                txn.direction = if (txn.amount < BigDecimal.ZERO) TransactionDirection.buy else TransactionDirection.redeem
            } else {
                txn.category = TransactionCategory.ordinary
                txn.direction = if (txn.amount < BigDecimal.ZERO) TransactionDirection.expense else TransactionDirection.income
            }

            // 替换摘要 - 匹配到第一条规则后停止
            val originalSummary = txn.txnTypeRawOriginal ?: txn.txnTypeRaw ?: ""
            val counterparty = txn.counterparty ?: ""

            for (rule in rules.replaceRules) {
                val ruleMatched = when (rule.matchType) {
                    "counterparty" -> counterparty.contains(rule.pattern, ignoreCase = true)
                    "both" -> {
                        val summaryMatch = originalSummary.contains(rule.pattern, ignoreCase = true)
                        val counterpartyMatch = rule.counterpartyPattern?.let {
                            counterparty.contains(it, ignoreCase = true)
                        } ?: false
                        summaryMatch && counterpartyMatch
                    }
                    else -> originalSummary.contains(rule.pattern, ignoreCase = true)
                }

                if (ruleMatched) {
                    txn.txnTypeRaw = rule.replacement
                    break // 匹配到第一条规则后立即退出
                }
            }
        }

        statementTransactionRepository.saveAll(transactions)
        return transactions.size
    }
}
