package com.investlab.service

import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.Strategy
import com.investlab.model.StrategyRequest
import com.investlab.repository.StrategyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class StrategyServiceTest {
    
    private lateinit var strategyRepository: StrategyRepository
    private lateinit var strategyService: StrategyService
    
    @BeforeEach
    fun setup() {
        strategyRepository = mockk()
        strategyService = StrategyService(strategyRepository)
    }
    
    @Test
    fun `should get all strategies`() {
        val strategies = listOf(
            Strategy(id = 1, name = "策略1", type = "index"),
            Strategy(id = 2, name = "策略2", type = "dividend")
        )
        every { strategyRepository.findAll() } returns strategies
        
        val result = strategyService.getAllStrategies()
        
        assertEquals(2, result.size)
        verify { strategyRepository.findAll() }
    }
    
    @Test
    fun `should get strategy by id`() {
        val strategy = Strategy(id = 1, name = "测试策略", type = "index")
        every { strategyRepository.findById(1) } returns Optional.of(strategy)
        
        val result = strategyService.getStrategyById(1)
        
        assertEquals("测试策略", result.name)
        verify { strategyRepository.findById(1) }
    }
    
    @Test
    fun `should throw exception when strategy not found`() {
        every { strategyRepository.findById(999) } returns Optional.empty()
        
        assertThrows<ResourceNotFoundException> {
            strategyService.getStrategyById(999)
        }
    }
    
    @Test
    fun `should create strategy`() {
        val request = StrategyRequest(
            name = "新策略",
            type = "grid",
            goalNote = "测试目标"
        )
        val savedStrategy = Strategy(
            id = 1,
            name = request.name,
            type = request.type,
            goalNote = request.goalNote
        )
        
        every { strategyRepository.save(any()) } returns savedStrategy
        
        val result = strategyService.createStrategy(request)
        
        assertEquals("新策略", result.name)
        assertEquals("grid", result.type)
        verify { strategyRepository.save(any()) }
    }
    
    @Test
    fun `should get strategies by type`() {
        val strategies = listOf(
            Strategy(id = 1, name = "指数策略1", type = "index"),
            Strategy(id = 2, name = "指数策略2", type = "index")
        )
        every { strategyRepository.findByType("index") } returns strategies
        
        val result = strategyService.getStrategiesByType("index")
        
        assertEquals(2, result.size)
        assertTrue(result.all { it.type == "index" })
        verify { strategyRepository.findByType("index") }
    }
}
