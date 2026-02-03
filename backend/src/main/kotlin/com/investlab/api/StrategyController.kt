package com.investlab.api

import com.investlab.model.StrategyRequest
import com.investlab.model.StrategyResponse
import com.investlab.service.StrategyService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/strategies")
class StrategyController(
    private val strategyService: StrategyService
) {
    
    @GetMapping
    fun getAllStrategies(@RequestParam(required = false) type: String?): ResponseEntity<List<StrategyResponse>> {
        val strategies = if (type != null) {
            strategyService.getStrategiesByType(type)
        } else {
            strategyService.getAllStrategies()
        }
        return ResponseEntity.ok(strategies)
    }
    
    @GetMapping("/{id}")
    fun getStrategyById(@PathVariable id: Long): ResponseEntity<StrategyResponse> {
        return ResponseEntity.ok(strategyService.getStrategyById(id))
    }
    
    @PostMapping
    fun createStrategy(@RequestBody request: StrategyRequest): ResponseEntity<StrategyResponse> {
        val created = strategyService.createStrategy(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
    
    @PutMapping("/{id}")
    fun updateStrategy(
        @PathVariable id: Long,
        @RequestBody request: StrategyRequest
    ): ResponseEntity<StrategyResponse> {
        return ResponseEntity.ok(strategyService.updateStrategy(id, request))
    }
    
    @DeleteMapping("/{id}")
    fun deleteStrategy(@PathVariable id: Long): ResponseEntity<Void> {
        strategyService.deleteStrategy(id)
        return ResponseEntity.noContent().build()
    }
    
    @GetMapping("/search")
    fun searchStrategies(@RequestParam name: String): ResponseEntity<List<StrategyResponse>> {
        return ResponseEntity.ok(strategyService.searchStrategies(name))
    }
    
    @GetMapping("/{id}/config")
    fun getStrategyConfig(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(strategyService.getStrategyConfig(id))
    }
    
    @PutMapping("/{id}/config")
    fun updateStrategyConfig(
        @PathVariable id: Long,
        @RequestBody config: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(strategyService.updateStrategyConfig(id, config))
    }
}
