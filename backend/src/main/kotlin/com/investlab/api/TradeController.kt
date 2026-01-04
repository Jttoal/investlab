package com.investlab.api

import com.investlab.model.TradeRequest
import com.investlab.model.TradeResponse
import com.investlab.service.TradeService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/trades")
class TradeController(
    private val tradeService: TradeService
) {
    
    @GetMapping
    fun getAllTrades(
        @RequestParam(required = false) strategyId: Long?,
        @RequestParam(required = false) accountId: Long?,
        @RequestParam(required = false) assetId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<List<TradeResponse>> {
        val trades = when {
            strategyId != null -> tradeService.getTradesByStrategy(strategyId)
            accountId != null -> tradeService.getTradesByAccount(accountId)
            assetId != null -> tradeService.getTradesByAsset(assetId)
            startDate != null && endDate != null -> tradeService.getTradesByDateRange(startDate, endDate)
            else -> tradeService.getAllTrades()
        }
        return ResponseEntity.ok(trades)
    }
    
    @GetMapping("/{id}")
    fun getTradeById(@PathVariable id: Long): ResponseEntity<TradeResponse> {
        return ResponseEntity.ok(tradeService.getTradeById(id))
    }
    
    @PostMapping
    fun createTrade(@RequestBody request: TradeRequest): ResponseEntity<TradeResponse> {
        val created = tradeService.createTrade(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
    
    @PutMapping("/{id}")
    fun updateTrade(
        @PathVariable id: Long,
        @RequestBody request: TradeRequest
    ): ResponseEntity<TradeResponse> {
        return ResponseEntity.ok(tradeService.updateTrade(id, request))
    }
    
    @DeleteMapping("/{id}")
    fun deleteTrade(@PathVariable id: Long): ResponseEntity<Void> {
        tradeService.deleteTrade(id)
        return ResponseEntity.noContent().build()
    }
}
