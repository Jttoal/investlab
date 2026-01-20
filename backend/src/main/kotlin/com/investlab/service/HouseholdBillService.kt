package com.investlab.service

import com.fasterxml.jackson.databind.ObjectMapper
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

                val (category, direction) = classify(row, keywords)
                toInsert.add(
                    StatementTransaction(
                        userId = userId,
                        fileId = statementFile.id!!,
                        txnDate = row.txnDate,
                        currency = row.currency,
                        amount = row.amount,
                        balance = row.balance,
                        txnTypeRaw = row.txnType,
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
        accountName: String?
    ): List<StatementTransactionResponse> {
        return statementTransactionRepository.search(startDate, endDate, category, direction, keyword, accountName)
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
        val accountPattern = Regex("户\\s*名[：:]+\\s*([^\\s]+)")
        Loader.loadPDF(path.toFile()).use { document ->
            val stripper = PDFTextStripper()
            for (page in 1..document.numberOfPages) {
                stripper.startPage = page
                stripper.endPage = page
                val text = stripper.getText(document) ?: continue
                if (page == 1 && accountName.isNullOrBlank()) {
                    val match = accountPattern.find(text)
                    accountName = match?.groupValues?.getOrNull(1)
                }
                val lines = text.split("\n")
                var pendingLine = ""
                var i = 0
                while (i < lines.size) {
                    val line = lines[i].trim()
                    if (line.isBlank()) {
                        i++
                        continue
                    }

                    if (isHeaderLine(line) || isEnglishHeaderLine(line) || isPageFooter(line)) {
                        pendingLine = ""
                        i++
                        continue
                    }

                    if (isDataLine(line)) {
                        val parsed = parseDataLine(line)
                        if (parsed != null) {
                            // 只有当对手信息列为空时，才拼接前后行
                            if (parsed.counterparty.isBlank()) {
                                var counterparty = ""
                                // 将前一行的内容添加到对手信息
                                if (pendingLine.isNotBlank()) {
                                    counterparty = pendingLine
                                }
                                
                                // 检查下一行是否是非数据行（对手信息续行）
                                val hasNext = (i + 1) < lines.size
                                if (hasNext) {
                                    val nextLine = lines[i + 1].trim()
                                    if (nextLine.isNotBlank()) {
                                        // 先检查是否是数据行（最常见）
                                        val nextIsData = isDataLine(nextLine)
                                        if (!nextIsData) {
                                            // 只有不是数据行时才继续检查其他类型
                                            val nextIsSpecial = isHeaderLine(nextLine) || isEnglishHeaderLine(nextLine) || isPageFooter(nextLine)
                                            if (!nextIsSpecial) {
                                                // 这是对手信息续行
                                                counterparty = if (counterparty.isNotBlank()) {
                                                    "$counterparty $nextLine"
                                                } else {
                                                    nextLine
                                                }
                                                i += 2  // 跳过当前行和下一行
                                            } else {
                                                i += 1
                                            }
                                        } else {
                                            i += 1
                                        }
                                    } else {
                                        i += 1
                                    }
                                } else {
                                    i += 1
                                }
                                rows.add(parsed.copy(counterparty = counterparty))
                            } else {
                                // 对手信息列不为空，说明没有自动换行，不需要拼接
                                rows.add(parsed)
                                i += 1
                            }
                            pendingLine = ""
                        } else {
                            // 解析失败，跳过这行
                            i += 1
                        }
                    } else {
                        // 非数据行，只有在不是表头关键词的情况下才保存为待处理行
                        // 避免将表头行的部分内容误当作对手信息
                        val containsHeaderKeywords = headerKeywords.any { line.contains(it) } || 
                                                     englishHeaderKeywords.any { line.contains(it) }
                        pendingLine = if (line.isNotBlank() && !containsHeaderKeywords) line else ""
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
}
