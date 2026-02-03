package com.investlab.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.*
import com.investlab.repository.StrategyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class StrategyService(
    private val strategyRepository: StrategyRepository,
    private val settingService: SettingService
) {
    
    private val objectMapper = jacksonObjectMapper()
    
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
    
    fun getStrategyConfig(id: Long): Map<String, Any> {
        // 验证策略存在
        if (!strategyRepository.existsById(id)) {
            throw ResourceNotFoundException("策略不存在: $id")
        }
        
        val configKey = "strategy_config_$id"
        val setting = settingService.getByKey(configKey)
        
        return if (setting != null) {
            objectMapper.readValue(setting.value, Map::class.java) as Map<String, Any>
        } else {
            // 返回默认配置
            mapOf(
                "maConfig" to mapOf(
                    "ma1" to 51,
                    "ma2" to 120,
                    "ma3" to 250,
                    "ma4" to 850
                ),
                "showBoll" to true
            )
        }
    }
    
    @Transactional
    fun updateStrategyConfig(id: Long, config: Map<String, Any>): Map<String, Any> {
        // 验证策略存在
        if (!strategyRepository.existsById(id)) {
            throw ResourceNotFoundException("策略不存在: $id")
        }
        
        val configKey = "strategy_config_$id"
        val configJson = objectMapper.writeValueAsString(config)
        
        val existingSetting = settingService.getByKey(configKey)
        
        if (existingSetting != null) {
            settingService.update(
                existingSetting.id,
                SettingRequest(
                    key = configKey,
                    value = configJson,
                    valueType = SettingValueType.json,
                    description = "策略 #$id 的配置"
                )
            )
        } else {
            settingService.create(
                SettingRequest(
                    key = configKey,
                    value = configJson,
                    valueType = SettingValueType.json,
                    description = "策略 #$id 的配置"
                )
            )
        }
        
        return config
    }
}
