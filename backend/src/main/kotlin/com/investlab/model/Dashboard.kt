package com.investlab.model

import java.math.BigDecimal

data class DashboardResponse(
    val totalAccounts: Int,
    val totalStrategies: Int,
    val totalAssets: Int,
    val totalTrades: Int,
    val accountSummaries: List<AccountSummary>,
    val strategySummaries: List<StrategySummary>
)

data class AccountSummary(
    val accountId: Long,
    val accountName: String,
    val broker: String,
    val balance: BigDecimal,
    val currency: String
)

data class StrategySummary(
    val strategyId: Long,
    val strategyName: String,
    val strategyType: String,
    val assetCount: Int,
    val tradeCount: Int,
    val totalInvested: BigDecimal,
    val totalValue: BigDecimal,
    val profitLoss: BigDecimal,
    val profitLossPercent: BigDecimal
)

data class StrategyDetailResponse(
    val strategy: StrategyResponse,
    val assets: List<AssetWithHolding>,
    val recentTrades: List<TradeResponse>,
    val recentViewpoints: List<ViewpointResponse>,
    val summary: StrategySummary
)

data class AssetWithHolding(
    val asset: AssetResponse,
    val holding: Int,
    val avgCost: BigDecimal,
    val currentPrice: BigDecimal?,
    val marketValue: BigDecimal,
    val profitLoss: BigDecimal,
    val profitLossPercent: BigDecimal,
    val alertStatus: String  // "none", "below_target", "above_target"
)
