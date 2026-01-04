package com.investlab.service

import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.Strategy
import com.investlab.model.StrategyRequest
import com.investlab.model.StrategyResponse
import com.investlab.repository.StrategyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class StrategyService(
    private val strategyRepository: StrategyRepository
) {
    
    fun getAllStrategies(): List<StrategyResponse> {
        return strategyRepository.findAll().map { StrategyResponse.from(it) }
    }
    
    fun getStrategyById(id: Long): StrategyResponse {
        val strategy = strategyRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("策略不存在: $id") }
        return StrategyResponse.from(strategy)
    }
    
    @Transactional
    fun createStrategy(request: StrategyRequest): StrategyResponse {
        val strategy = Strategy(
            name = request.name,
            type = request.type,
            goalNote = request.goalNote
        )
        val saved = strategyRepository.save(strategy)
        return StrategyResponse.from(saved)
    }
    
    @Transactional
    fun updateStrategy(id: Long, request: StrategyRequest): StrategyResponse {
        val strategy = strategyRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("策略不存在: $id") }
        
        strategy.name = request.name
        strategy.type = request.type
        strategy.goalNote = request.goalNote
        strategy.updatedAt = LocalDateTime.now()
        
        val updated = strategyRepository.save(strategy)
        return StrategyResponse.from(updated)
    }
    
    @Transactional
    fun deleteStrategy(id: Long) {
        if (!strategyRepository.existsById(id)) {
            throw ResourceNotFoundException("策略不存在: $id")
        }
        strategyRepository.deleteById(id)
    }
    
    fun getStrategiesByType(type: String): List<StrategyResponse> {
        return strategyRepository.findByType(type)
            .map { StrategyResponse.from(it) }
    }
    
    fun searchStrategies(name: String): List<StrategyResponse> {
        return strategyRepository.findByNameContaining(name)
            .map { StrategyResponse.from(it) }
    }
}
