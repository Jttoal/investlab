package com.investlab.service

import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.Trade
import com.investlab.model.TradeRequest
import com.investlab.model.TradeResponse
import com.investlab.repository.AccountRepository
import com.investlab.repository.AssetRepository
import com.investlab.repository.StrategyRepository
import com.investlab.repository.TradeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class TradeService(
    private val tradeRepository: TradeRepository,
    private val strategyRepository: StrategyRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository
) {
    
    fun getAllTrades(): List<TradeResponse> {
        return tradeRepository.findAll().map { TradeResponse.from(it) }
    }
    
    fun getTradeById(id: Long): TradeResponse {
        val trade = tradeRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("交易记录不存在: $id") }
        return TradeResponse.from(trade)
    }
    
    @Transactional
    fun createTrade(request: TradeRequest): TradeResponse {
        // 验证关联实体是否存在
        validateTradeReferences(request.strategyId, request.accountId, request.assetId)
        
        val trade = Trade(
            strategyId = request.strategyId,
            accountId = request.accountId,
            assetId = request.assetId,
            type = request.type,
            price = request.price,
            quantity = request.quantity,
            fee = request.fee,
            tradeDate = request.tradeDate,
            note = request.note
        )
        val saved = tradeRepository.save(trade)
        return TradeResponse.from(saved)
    }
    
    @Transactional
    fun updateTrade(id: Long, request: TradeRequest): TradeResponse {
        val trade = tradeRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("交易记录不存在: $id") }
        
        // 验证关联实体是否存在
        validateTradeReferences(request.strategyId, request.accountId, request.assetId)
        
        trade.strategyId = request.strategyId
        trade.accountId = request.accountId
        trade.assetId = request.assetId
        trade.type = request.type
        trade.price = request.price
        trade.quantity = request.quantity
        trade.fee = request.fee
        trade.tradeDate = request.tradeDate
        trade.note = request.note
        trade.updatedAt = LocalDateTime.now()
        
        val updated = tradeRepository.save(trade)
        return TradeResponse.from(updated)
    }
    
    @Transactional
    fun deleteTrade(id: Long) {
        if (!tradeRepository.existsById(id)) {
            throw ResourceNotFoundException("交易记录不存在: $id")
        }
        tradeRepository.deleteById(id)
    }
    
    fun getTradesByStrategy(strategyId: Long): List<TradeResponse> {
        return tradeRepository.findByStrategyIdOrderByTradeDateDesc(strategyId)
            .map { TradeResponse.from(it) }
    }
    
    fun getTradesByAccount(accountId: Long): List<TradeResponse> {
        return tradeRepository.findByAccountId(accountId)
            .map { TradeResponse.from(it) }
    }
    
    fun getTradesByAsset(assetId: Long): List<TradeResponse> {
        return tradeRepository.findByAssetId(assetId)
            .map { TradeResponse.from(it) }
    }
    
    fun getTradesByDateRange(startDate: LocalDate, endDate: LocalDate): List<TradeResponse> {
        return tradeRepository.findByTradeDateBetween(startDate, endDate)
            .map { TradeResponse.from(it) }
    }
    
    private fun validateTradeReferences(strategyId: Long, accountId: Long, assetId: Long) {
        if (!strategyRepository.existsById(strategyId)) {
            throw ResourceNotFoundException("策略不存在: $strategyId")
        }
        if (!accountRepository.existsById(accountId)) {
            throw ResourceNotFoundException("账户不存在: $accountId")
        }
        if (!assetRepository.existsById(assetId)) {
            throw ResourceNotFoundException("标的不存在: $assetId")
        }
    }
}
