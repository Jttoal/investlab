package com.investlab.api

import com.investlab.model.AssetRequest
import com.investlab.model.AssetResponse
import com.investlab.service.AssetService
import com.investlab.service.ViewpointService
import com.investlab.repository.TradeRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/assets")
class AssetController(
    private val assetService: AssetService,
    private val viewpointService: ViewpointService,
    private val tradeRepository: TradeRepository
) {
    
    @GetMapping
    fun getAllAssets(@RequestParam(required = false) strategyId: Long?): ResponseEntity<List<AssetResponse>> {
        val assets = if (strategyId != null) {
            assetService.getAssetsByStrategy(strategyId)
        } else {
            assetService.getAllAssets()
        }
        return ResponseEntity.ok(assets)
    }
    
    @GetMapping("/{id}")
    fun getAssetById(@PathVariable id: Long): ResponseEntity<AssetResponse> {
        return ResponseEntity.ok(assetService.getAssetById(id))
    }

    @GetMapping("/{id}/detail")
    fun getAssetDetail(@PathVariable id: Long): ResponseEntity<com.investlab.model.AssetDetailResponse> {
        return ResponseEntity.ok(assetService.getAssetDetail(id))
    }

    @GetMapping("/{id}/kline")
    fun getKline(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "day") period: String,
        @RequestParam(required = false) start: String?,
        @RequestParam(required = false) end: String?
    ): ResponseEntity<List<com.investlab.model.KlinePoint>> {
        val startDate = start?.let { LocalDate.parse(it) }
        val endDate = end?.let { LocalDate.parse(it) }
        return ResponseEntity.ok(assetService.getKlines(id, period, startDate, endDate))
    }

    @GetMapping("/{id}/kline/bundle")
    fun getKlineBundle(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "day") period: String,
        @RequestParam(required = false) start: String?,
        @RequestParam(required = false) end: String?
    ): ResponseEntity<com.investlab.model.AssetKlineResponse> {
        val startDate = start?.let { LocalDate.parse(it) } ?: LocalDate.now().minusYears(1)
        val endDate = end?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val viewpoints = viewpointService.getViewpointsByAsset(id)
        val trades = tradeRepository.findByAssetIdAndTradeDateBetween(id, startDate, endDate)
        val bundle = assetService.getKlineBundle(id, period, startDate, endDate, trades, viewpoints)
        return ResponseEntity.ok(bundle)
    }
    
    @PostMapping
    fun createAsset(@RequestBody request: AssetRequest): ResponseEntity<AssetResponse> {
        val created = assetService.createAsset(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
    
    @PutMapping("/{id}")
    fun updateAsset(
        @PathVariable id: Long,
        @RequestBody request: AssetRequest
    ): ResponseEntity<AssetResponse> {
        return ResponseEntity.ok(assetService.updateAsset(id, request))
    }
    
    @DeleteMapping("/{id}")
    fun deleteAsset(@PathVariable id: Long): ResponseEntity<Void> {
        assetService.deleteAsset(id)
        return ResponseEntity.noContent().build()
    }
    
    @GetMapping("/search")
    fun searchAssets(@RequestParam symbol: String): ResponseEntity<List<AssetResponse>> {
        return ResponseEntity.ok(assetService.getAssetsBySymbol(symbol))
    }
    
    @GetMapping("/{id}/config")
    fun getAssetConfig(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(assetService.getAssetConfig(id))
    }
    
    @PutMapping("/{id}/config")
    fun updateAssetConfig(
        @PathVariable id: Long,
        @RequestBody config: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(assetService.updateAssetConfig(id, config))
    }
}
