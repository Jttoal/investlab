package com.investlab.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1")
class HealthController {
    
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "application" to "InvestLab",
            "version" to "1.0.0-MVP"
        ))
    }
    
    @GetMapping
    fun welcome(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "message" to "Welcome to InvestLab API",
            "version" to "v1",
            "docs" to "/api/v1/health"
        ))
    }
}
