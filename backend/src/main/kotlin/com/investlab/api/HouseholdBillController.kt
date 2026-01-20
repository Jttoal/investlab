package com.investlab.api

import com.investlab.model.StatementFileResponse
import com.investlab.model.StatementSummaryResponse
import com.investlab.model.StatementTransactionResponse
import com.investlab.model.TransactionCategory
import com.investlab.model.TransactionDirection
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
        @RequestParam(required = false) accountName: String?
    ): ResponseEntity<List<StatementTransactionResponse>> {
        return ResponseEntity.ok(
            householdBillService.listTransactions(startDate, endDate, category, direction, keyword, accountName)
        )
    }

    @GetMapping("/summary")
    fun summarize(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<List<StatementSummaryResponse>> {
        return ResponseEntity.ok(householdBillService.summarize(startDate, endDate))
    }
}
