package com.investlab.api

import com.investlab.model.*
import com.investlab.service.HouseholdBillService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/household-bills")
class HouseholdBillController(
    private val householdBillService: HouseholdBillService
) {

    @PostMapping("/uploads")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false, defaultValue = "0") userId: Long
    ): ResponseEntity<StatementFileResponse> {
        val result = householdBillService.upload(file, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @GetMapping("/uploads/{id}")
    fun getUpload(@PathVariable id: Long): ResponseEntity<StatementFileResponse> {
        return ResponseEntity.ok(householdBillService.getFile(id))
    }

    @GetMapping("/transactions")
    fun listTransactions(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @RequestParam(required = false) category: TransactionCategory?,
        @RequestParam(required = false) direction: TransactionDirection?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) accountName: String?,
        @RequestParam(required = false) counterparty: String?,
        @RequestParam(required = false) amountDirection: String?
    ): ResponseEntity<List<StatementTransactionResponse>> {
        return ResponseEntity.ok(
            householdBillService.listTransactions(startDate, endDate, category, direction, keyword, accountName, counterparty, amountDirection)
        )
    }

    @GetMapping("/summary")
    fun summarize(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<List<StatementSummaryResponse>> {
        return ResponseEntity.ok(householdBillService.summarize(startDate, endDate))
    }

    // 新增统计接口
    @GetMapping("/statistics/monthly")
    fun getMonthlySummary(
        @RequestParam month: String,  // YYYY-MM
        @RequestParam(defaultValue = "ordinary") category: String,
        @RequestParam(required = false) accountNames: List<String>?
    ): ResponseEntity<com.investlab.model.MonthlySummaryResponse> {
        return ResponseEntity.ok(householdBillService.getMonthlySummary(month, category, accountNames))
    }

    @GetMapping("/statistics/yearly")
    fun getYearlySummary(
        @RequestParam year: String,  // YYYY
        @RequestParam(defaultValue = "ordinary") category: String,
        @RequestParam(required = false) accountNames: List<String>?
    ): ResponseEntity<com.investlab.model.MonthlySummaryResponse> {
        return ResponseEntity.ok(householdBillService.getYearlySummary(year, category, accountNames))
    }

    @GetMapping("/statistics/monthly-trend")
    fun getMonthlyTrend(
        @RequestParam year: String,
        @RequestParam(defaultValue = "ordinary") category: String,
        @RequestParam(required = false) accountNames: List<String>?
    ): ResponseEntity<com.investlab.model.TrendResponse> {
        return ResponseEntity.ok(householdBillService.getMonthlyTrend(year, category, accountNames))
    }

    @GetMapping("/statistics/yearly-trend")
    fun getYearlyTrend(
        @RequestParam(defaultValue = "ordinary") category: String,
        @RequestParam(required = false) accountNames: List<String>?
    ): ResponseEntity<com.investlab.model.TrendResponse> {
        return ResponseEntity.ok(householdBillService.getYearlyTrend(category, accountNames))
    }

    @PatchMapping("/transactions/{id}/summary")
    fun updateTransactionSummary(
        @PathVariable id: Long,
        @RequestBody request: UpdateTransactionSummaryRequest,
        @RequestParam(required = false, defaultValue = "0") userId: Long
    ): ResponseEntity<StatementTransactionResponse> {
        return ResponseEntity.ok(householdBillService.updateTransactionSummary(userId, id, request.txnTypeRaw))
    }

    @GetMapping("/account-names")
    fun getAccountNames(
        @RequestParam(required = false, defaultValue = "0") userId: Long
    ): ResponseEntity<List<String>> {
        return ResponseEntity.ok(householdBillService.getAccountNames(userId))
    }

    @GetMapping("/rules")
    fun getRules(
        @RequestParam(required = false, defaultValue = "0") userId: Long
    ): ResponseEntity<ImportRulesResponse> {
        return ResponseEntity.ok(householdBillService.getRules(userId))
    }

    @PutMapping("/rules")
    fun saveRules(
        @RequestBody request: ImportRulesRequest,
        @RequestParam(required = false, defaultValue = "0") userId: Long
    ): ResponseEntity<ImportRulesResponse> {
        return ResponseEntity.ok(householdBillService.saveRules(userId, request))
    }

    @PostMapping("/rules/preview-rerun")
    fun previewRerun(
        @RequestParam(required = false, defaultValue = "0") userId: Long
    ): ResponseEntity<List<RerunChangeResponse>> {
        return ResponseEntity.ok(householdBillService.previewRerun(userId))
    }

    @PostMapping("/rules/execute-rerun")
    fun executeRerun(
        @RequestBody request: ExecuteRerunRequest,
        @RequestParam(required = false, defaultValue = "0") userId: Long
    ): ResponseEntity<Map<String, Int>> {
        val count = householdBillService.executeRerun(userId, request.changeIds)
        return ResponseEntity.ok(mapOf("count" to count))
    }
}
