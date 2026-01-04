package com.investlab.service

import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.*
import com.investlab.repository.AssetRepository
import com.investlab.repository.StrategyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AssetService(
    private val assetRepository: AssetRepository,
    private val strategyRepository: StrategyRepository,
    private val snowballClient: SnowballClient
) {
    
    fun getAllAssets(): List<AssetResponse> {
        return assetRepository.findAll().map { AssetResponse.from(it) }
    }
    
    fun getAssetById(id: Long): AssetResponse {
        val asset = assetRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("标的不存在: $id") }
        return AssetResponse.from(asset)
    }
    
    @Transactional
    fun createAsset(request: AssetRequest): AssetResponse {
        // 验证策略是否存在
        if (!strategyRepository.existsById(request.strategyId)) {
            throw ResourceNotFoundException("策略不存在: ${request.strategyId}")
        }
        
        val asset = Asset(
            symbol = request.symbol,
            name = request.name,
            market = request.market,
            strategyId = request.strategyId,
            targetLow = request.targetLow,
            targetHigh = request.targetHigh,
            note = request.note
        )
        val saved = assetRepository.save(asset)
        return AssetResponse.from(saved)
    }
    
    @Transactional
    fun updateAsset(id: Long, request: AssetRequest): AssetResponse {
        val asset = assetRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("标的不存在: $id") }
        
        // 验证策略是否存在
        if (!strategyRepository.existsById(request.strategyId)) {
            throw ResourceNotFoundException("策略不存在: ${request.strategyId}")
        }
        
        asset.symbol = request.symbol
        asset.name = request.name
        asset.market = request.market
        asset.strategyId = request.strategyId
        asset.targetLow = request.targetLow
        asset.targetHigh = request.targetHigh
        asset.note = request.note
        asset.updatedAt = LocalDateTime.now()
        
        val updated = assetRepository.save(asset)
        return AssetResponse.from(updated)
    }
    
    @Transactional
    fun deleteAsset(id: Long) {
        if (!assetRepository.existsById(id)) {
            throw ResourceNotFoundException("标的不存在: $id")
        }
        assetRepository.deleteById(id)
    }
    
    fun getAssetsByStrategy(strategyId: Long): List<AssetResponse> {
        return assetRepository.findByStrategyId(strategyId)
            .map { AssetResponse.from(it) }
    }
    
    fun getAssetsBySymbol(symbol: String): List<AssetResponse> {
        return assetRepository.findBySymbol(symbol)
            .map { AssetResponse.from(it) }
    }

    fun getAssetDetail(id: Long): AssetDetailResponse {
        val asset = assetRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("标的不存在: $id") }
        val strategy = strategyRepository.findById(asset.strategyId).orElse(null)
        return AssetDetailResponse(
            asset = AssetResponse.from(asset),
            strategy = strategy?.let { StrategyResponse.from(it) }
        )
    }

    fun getKlines(assetId: Long, period: String, start: LocalDate?, end: LocalDate?): List<KlinePoint> {
        val asset = assetRepository.findById(assetId)
            .orElseThrow { ResourceNotFoundException("标的不存在: $assetId") }
        val symbol = normalizeSymbol(asset)
        val all = try {
            snowballClient.fetchKlines(symbol, period.lowercase())
        } catch (ex: Exception) {
            // 降级为 mock，确保前端可联调
            generateMockKlines(period.lowercase())
        }
        val startDate = start ?: LocalDate.now().minusYears(1)
        val endDate = end ?: LocalDate.now()
        return all.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
    }

    fun getKlineBundle(
        assetId: Long,
        period: String,
        start: LocalDate?,
        end: LocalDate?,
        trades: List<Trade>,
        viewpoints: List<ViewpointResponse>
    ): AssetKlineResponse {
        val kline = getKlines(assetId, period, start, end)
        val holding = calculateHolding(trades)
        return AssetKlineResponse(
            kline = kline,
            viewpoints = viewpoints,
            trades = trades.map { TradeResponse.from(it) },
            holding = holding
        )
    }

    private fun calculateHolding(trades: List<Trade>): HoldingSummary? {
        if (trades.isEmpty()) return null
        var totalQuantity = 0
        var totalCost = BigDecimal.ZERO
        trades.sortedBy { it.tradeDate }.forEach { trade ->
            when (trade.type.lowercase()) {
                "buy" -> {
                    totalQuantity += trade.quantity
                    totalCost = totalCost.add(trade.price.multiply(BigDecimal(trade.quantity)).add(trade.fee))
                }
                "sell" -> {
                    totalQuantity -= trade.quantity
                    if (totalQuantity > 0) {
                        val sellRatio = BigDecimal(trade.quantity).divide(BigDecimal(totalQuantity + trade.quantity), 4, RoundingMode.HALF_UP)
                        totalCost = totalCost.multiply(BigDecimal.ONE.subtract(sellRatio))
                    } else {
                        totalCost = BigDecimal.ZERO
                    }
                }
                "dividend" -> {
                    totalCost = totalCost.subtract(trade.price.multiply(BigDecimal(trade.quantity)))
                }
            }
        }
        val avgCost = if (totalQuantity > 0) totalCost.divide(BigDecimal(totalQuantity), 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val lastClose = trades.maxByOrNull { it.tradeDate }?.price ?: avgCost
        val marketValue = lastClose.multiply(BigDecimal(totalQuantity))
        val profitLoss = marketValue.subtract(totalCost)
        val profitLossPercent = if (totalCost > BigDecimal.ZERO) profitLoss.divide(totalCost, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
        return HoldingSummary(
            holding = totalQuantity,
            avgCost = avgCost,
            currentPrice = lastClose,
            marketValue = marketValue,
            profitLoss = profitLoss,
            profitLossPercent = profitLossPercent
        )
    }

    private fun normalizeSymbol(asset: Asset): String {
        // 简单映射：A股 CN -> SH/SZ 根据代码首位判定；HK/US 可按需要扩展
        return when (asset.market.uppercase()) {
            "CN" -> {
                val code = asset.symbol
                if (code.startsWith("6")) "SH$code" else "SZ$code"
            }
            "HK" -> "HK${asset.symbol}"
            "US" -> asset.symbol
            else -> asset.symbol
        }
    }

    private fun generateMockKlines(period: String): List<KlinePoint> {
        val days = when (period) {
            "week" -> 260
            "month" -> 120
            "year" -> 30
            else -> 400
        }
        val today = LocalDate.now()
        val base = BigDecimal("10.0")
        val points = mutableListOf<KlinePoint>()
        var lastClose = base
        repeat(days) { i ->
            val date = today.minusDays((days - i).toLong())
            val drift = BigDecimal(Math.sin(i / 20.0)).setScale(4, RoundingMode.HALF_UP)
            val noise = BigDecimal((Math.random() - 0.5) / 10).setScale(4, RoundingMode.HALF_UP)
            val open = lastClose
            val close = (lastClose + drift + noise).max(BigDecimal("1.0"))
            val high = listOf(open, close).maxOrNull()!!.plus(BigDecimal("0.2"))
            val low = listOf(open, close).minOrNull()!!.minus(BigDecimal("0.2"))
            val volume = BigDecimal((1000..5000).random())
            lastClose = close
            points.add(
                KlinePoint(
                    date = date,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
        }
        return applyIndicators(points)
    }

    private fun applyIndicators(list: List<KlinePoint>): List<KlinePoint> {
        val closes = list.map { it.close }
        val ma = fun(window: Int, idx: Int): BigDecimal? {
            if (idx + 1 < window) return null
            val slice = closes.subList(idx + 1 - window, idx + 1)
            return slice.reduce { acc, v -> acc + v }
                .divide(BigDecimal(window), 4, RoundingMode.HALF_UP)
        }
        val period = 20
        val k = BigDecimal("2")

        return list.mapIndexed { idx, p ->
            val ma20 = if (idx + 1 < period) null else closes.subList(idx + 1 - period, idx + 1)
                .reduce { acc, v -> acc + v }
                .divide(BigDecimal(period), 4, RoundingMode.HALF_UP)
            val std = if (idx + 1 < period) null else {
                val slice = closes.subList(idx + 1 - period, idx + 1)
                val mean = ma20!!
                val variance = slice.map { (it - mean).pow(2) }
                    .reduce { acc, v -> acc + v }
                    .divide(BigDecimal(period), 6, RoundingMode.HALF_UP)
                variance.sqrt()
            }
            p.copy(
                ma51 = ma(51, idx),
                ma120 = ma(120, idx),
                ma250 = ma(250, idx),
                ma850 = ma(850, idx),
                bollMid = ma20,
                bollUpper = if (ma20 != null && std != null) ma20 + k * std else null,
                bollLower = if (ma20 != null && std != null) ma20 - k * std else null
            )
        }
    }

    private fun BigDecimal.sqrt(): BigDecimal {
        var g = BigDecimal(Math.sqrt(this.toDouble()))
        repeat(10) {
            g = (g + this.divide(g, 10, RoundingMode.HALF_UP)).divide(BigDecimal("2"), 10, RoundingMode.HALF_UP)
        }
        return g
    }
}
