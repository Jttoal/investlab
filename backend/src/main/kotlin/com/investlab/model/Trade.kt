package com.investlab.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "trade")
data class Trade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "strategy_id", nullable = false)
    var strategyId: Long,
    
    @Column(name = "account_id", nullable = false)
    var accountId: Long,
    
    @Column(name = "asset_id", nullable = false)
    var assetId: Long,
    
    @Column(nullable = false, length = 20)
    var type: String,
    
    @Column(nullable = false, precision = 10, scale = 4)
    var price: BigDecimal,
    
    @Column(nullable = false)
    var quantity: Int,
    
    @Column(nullable = false, precision = 10, scale = 2)
    var fee: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "trade_date", nullable = false)
    var tradeDate: LocalDate,
    
    @Column(columnDefinition = "TEXT")
    var note: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

data class TradeRequest(
    val strategyId: Long,
    val accountId: Long,
    val assetId: Long,
    val type: String,
    val price: BigDecimal,
    val quantity: Int,
    val fee: BigDecimal = BigDecimal.ZERO,
    val tradeDate: LocalDate,
    val note: String? = null
)

data class TradeResponse(
    val id: Long,
    val strategyId: Long,
    val accountId: Long,
    val assetId: Long,
    val type: String,
    val price: BigDecimal,
    val quantity: Int,
    val fee: BigDecimal,
    val tradeDate: LocalDate,
    val note: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(trade: Trade): TradeResponse {
            return TradeResponse(
                id = trade.id!!,
                strategyId = trade.strategyId,
                accountId = trade.accountId,
                assetId = trade.assetId,
                type = trade.type,
                price = trade.price,
                quantity = trade.quantity,
                fee = trade.fee,
                tradeDate = trade.tradeDate,
                note = trade.note,
                createdAt = trade.createdAt,
                updatedAt = trade.updatedAt
            )
        }
    }
}
