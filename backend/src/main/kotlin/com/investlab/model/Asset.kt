package com.investlab.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "asset")
data class Asset(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, length = 50)
    var symbol: String,
    
    @Column(nullable = false, length = 100)
    var name: String,
    
    @Column(nullable = false, length = 10)
    var market: String,
    
    @Column(name = "strategy_id", nullable = false)
    var strategyId: Long,
    
    @Column(name = "target_low", precision = 10, scale = 2)
    var targetLow: BigDecimal? = null,
    
    @Column(name = "target_high", precision = 10, scale = 2)
    var targetHigh: BigDecimal? = null,
    
    @Column(columnDefinition = "TEXT")
    var note: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

data class AssetRequest(
    val symbol: String,
    val name: String,
    val market: String,
    val strategyId: Long,
    val targetLow: BigDecimal? = null,
    val targetHigh: BigDecimal? = null,
    val note: String? = null
)

data class AssetResponse(
    val id: Long,
    val symbol: String,
    val name: String,
    val market: String,
    val strategyId: Long,
    val targetLow: BigDecimal?,
    val targetHigh: BigDecimal?,
    val note: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(asset: Asset): AssetResponse {
            return AssetResponse(
                id = asset.id!!,
                symbol = asset.symbol,
                name = asset.name,
                market = asset.market,
                strategyId = asset.strategyId,
                targetLow = asset.targetLow,
                targetHigh = asset.targetHigh,
                note = asset.note,
                createdAt = asset.createdAt,
                updatedAt = asset.updatedAt
            )
        }
    }
}
