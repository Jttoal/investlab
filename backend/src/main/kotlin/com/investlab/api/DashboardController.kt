package com.investlab.api

import com.investlab.model.DashboardResponse
import com.investlab.model.StrategyDetailResponse
import com.investlab.service.DashboardService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    private val dashboardService: DashboardService
) {
    
    @GetMapping
    fun getDashboard(): ResponseEntity<DashboardResponse> {
        return ResponseEntity.ok(dashboardService.getDashboard())
    }
    
    @GetMapping("/strategies/{strategyId}")
    fun getStrategyDetail(@PathVariable strategyId: Long): ResponseEntity<StrategyDetailResponse> {
        return ResponseEntity.ok(dashboardService.getStrategyDetail(strategyId))
    }
}
