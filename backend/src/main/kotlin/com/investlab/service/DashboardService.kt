package com.investlab.service

import com.investlab.model.*
import com.investlab.repository.*
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class DashboardService(
    private val accountRepository: AccountRepository,
    private val strategyRepository: StrategyRepository,
    private val assetRepository: AssetRepository,
    private val tradeRepository: TradeRepository,
    private val viewpointRepository: ViewpointRepository
) {
    
    fun getDashboard(): DashboardResponse {
        val accounts = accountRepository.findAll()
        val strategies = strategyRepository.findAll()
        
        val accountSummaries = accounts.map { account ->
            AccountSummary(
                accountId = account.id!!,
                accountName = account.name,
                broker = account.broker,
                balance = account.balanceManual,
                currency = account.currency
            )
        }
        
        val strategySummaries = strategies.map { strategy ->
            calculateStrategySummary(strategy.id!!)
        }
        
        return DashboardResponse(
            totalAccounts = accounts.size,
            totalStrategies = strategies.size,
            totalAssets = assetRepository.count().toInt(),
            totalTrades = tradeRepository.count().toInt(),
            accountSummaries = accountSummaries,
            strategySummaries = strategySummaries
        )
    }
    
    fun getStrategyDetail(strategyId: Long): StrategyDetailResponse {
        val strategy = strategyRepository.findById(strategyId)
            .orElseThrow { throw RuntimeException("策略不存在: $strategyId") }
        
        val assets = assetRepository.findByStrategyId(strategyId)
        val trades = tradeRepository.findByStrategyIdOrderByTradeDateDesc(strategyId)
        val viewpoints = viewpointRepository.findByStrategyIdOrderByDateDesc(strategyId)
        
        val assetsWithHolding = assets.map { asset ->
            calculateAssetHolding(asset, trades.filter { it.assetId == asset.id })
        }
        
        return StrategyDetailResponse(
            strategy = StrategyResponse.from(strategy),
            assets = assetsWithHolding,
            recentTrades = trades.take(10).map { TradeResponse.from(it) },
            recentViewpoints = viewpoints.take(5).map { ViewpointResponse.from(it) },
            summary = calculateStrategySummary(strategyId)
        )
    }
    
    private fun calculateStrategySummary(strategyId: Long): StrategySummary {
        val strategy = strategyRepository.findById(strategyId).orElseThrow()
        val assets = assetRepository.findByStrategyId(strategyId)
        val trades = tradeRepository.findByStrategyId(strategyId)
        
        var totalInvested = BigDecimal.ZERO
        var totalValue = BigDecimal.ZERO
        
        assets.forEach { asset ->
            val assetTrades = trades.filter { it.assetId == asset.id }
            val holding = calculateAssetHolding(asset, assetTrades)
            totalInvested = totalInvested.add(holding.avgCost.multiply(BigDecimal(holding.holding)))
            totalValue = totalValue.add(holding.marketValue)
        }
        
        val profitLoss = totalValue.subtract(totalInvested)
        val profitLossPercent = if (totalInvested > BigDecimal.ZERO) {
            profitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        return StrategySummary(
            strategyId = strategyId,
            strategyName = strategy.name,
            strategyType = strategy.type,
            assetCount = assets.size,
            tradeCount = trades.size,
            totalInvested = totalInvested,
            totalValue = totalValue,
            profitLoss = profitLoss,
            profitLossPercent = profitLossPercent
        )
    }
    
    private fun calculateAssetHolding(asset: Asset, trades: List<Trade>): AssetWithHolding {
        var totalQuantity = 0
        var totalCost = BigDecimal.ZERO
        
        trades.forEach { trade ->
            when (trade.type.lowercase()) {
                "buy" -> {
                    totalQuantity += trade.quantity
                    totalCost = totalCost.add(
                        trade.price.multiply(BigDecimal(trade.quantity)).add(trade.fee)
                    )
                }
                "sell" -> {
                    totalQuantity -= trade.quantity
                    // 简化处理:卖出时按比例减少成本
                    if (totalQuantity > 0) {
                        val sellRatio = BigDecimal(trade.quantity).divide(
                            BigDecimal(totalQuantity + trade.quantity), 
                            4, 
                            RoundingMode.HALF_UP
                        )
                        totalCost = totalCost.multiply(BigDecimal.ONE.subtract(sellRatio))
                    } else {
                        totalCost = BigDecimal.ZERO
                    }
                }
                "dividend" -> {
                    // 分红减少成本
                    totalCost = totalCost.subtract(
                        trade.price.multiply(BigDecimal(trade.quantity))
                    )
                }
            }
        }
        
        val avgCost = if (totalQuantity > 0) {
            totalCost.divide(BigDecimal(totalQuantity), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        // MVP阶段:使用最后一笔交易价格作为当前价格
        val currentPrice = trades.maxByOrNull { it.tradeDate }?.price ?: avgCost
        val marketValue = currentPrice.multiply(BigDecimal(totalQuantity))
        val profitLoss = marketValue.subtract(totalCost)
        val profitLossPercent = if (totalCost > BigDecimal.ZERO) {
            profitLoss.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
        
        val alertStatus = when {
            asset.targetLow != null && currentPrice < asset.targetLow -> "below_target"
            asset.targetHigh != null && currentPrice > asset.targetHigh -> "above_target"
            else -> "none"
        }
        
        return AssetWithHolding(
            asset = AssetResponse.from(asset),
            holding = totalQuantity,
            avgCost = avgCost,
            currentPrice = currentPrice,
            marketValue = marketValue,
            profitLoss = profitLoss,
            profitLossPercent = profitLossPercent,
            alertStatus = alertStatus
        )
    }
}
