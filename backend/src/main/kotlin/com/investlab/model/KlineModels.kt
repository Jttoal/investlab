package com.investlab.model

import java.math.BigDecimal
import java.time.LocalDate

data class KlinePoint(
    val date: LocalDate,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal,
    val ma1: BigDecimal? = null,
    val ma2: BigDecimal? = null,
    val ma3: BigDecimal? = null,
    val ma4: BigDecimal? = null,
    val bollUpper: BigDecimal? = null,
    val bollMid: BigDecimal? = null,
    val bollLower: BigDecimal? = null
)

data class AssetDetailResponse(
    val asset: AssetResponse,
    val strategy: StrategyResponse?
)

enum class SettingValueType { string, number, json }

data class HoldingSummary(
    val holding: Int,
    val avgCost: BigDecimal,
    val currentPrice: BigDecimal?,
    val marketValue: BigDecimal,
    val profitLoss: BigDecimal,
    val profitLossPercent: BigDecimal
)

data class AssetKlineResponse(
    val kline: List<KlinePoint>,
    val viewpoints: List<ViewpointResponse>,
    val trades: List<TradeResponse>,
    val holding: HoldingSummary?,
    val maConfig: MaConfig? = null
)

data class MaConfig(
    val ma1: Int = 51,
    val ma2: Int = 120,
    val ma3: Int = 250,
    val ma4: Int = 850
)

data class SettingRequest(
    val key: String,
    val value: String,
    val valueType: SettingValueType = SettingValueType.string,
    val description: String? = null
)

data class SettingResponse(
    val id: Long,
    val key: String,
    val value: String,
    val valueType: SettingValueType,
    val description: String?,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime
)
