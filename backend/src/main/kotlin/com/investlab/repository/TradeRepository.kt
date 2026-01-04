package com.investlab.repository

import com.investlab.model.Trade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TradeRepository : JpaRepository<Trade, Long> {
    fun findByStrategyId(strategyId: Long): List<Trade>
    fun findByAccountId(accountId: Long): List<Trade>
    fun findByAssetId(assetId: Long): List<Trade>
    fun findByStrategyIdAndAssetId(strategyId: Long, assetId: Long): List<Trade>
    fun findByTradeDateBetween(startDate: LocalDate, endDate: LocalDate): List<Trade>
    fun findByAssetIdAndTradeDateBetween(assetId: Long, startDate: LocalDate, endDate: LocalDate): List<Trade>
    
    @Query("SELECT t FROM Trade t WHERE t.strategyId = :strategyId ORDER BY t.tradeDate DESC")
    fun findByStrategyIdOrderByTradeDateDesc(strategyId: Long): List<Trade>
}
