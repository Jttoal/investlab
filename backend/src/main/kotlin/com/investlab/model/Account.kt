package com.investlab.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "account")
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, length = 100)
    var name: String,
    
    @Column(nullable = false, length = 100)
    var broker: String,
    
    @Column(nullable = false, length = 10)
    var currency: String = "CNY",
    
    @Column(name = "balance_manual", nullable = false, precision = 15, scale = 2)
    var balanceManual: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

data class AccountRequest(
    val name: String,
    val broker: String,
    val currency: String = "CNY",
    val balanceManual: BigDecimal = BigDecimal.ZERO
)

data class AccountResponse(
    val id: Long,
    val name: String,
    val broker: String,
    val currency: String,
    val balanceManual: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(account: Account): AccountResponse {
            return AccountResponse(
                id = account.id!!,
                name = account.name,
                broker = account.broker,
                currency = account.currency,
                balanceManual = account.balanceManual,
                createdAt = account.createdAt,
                updatedAt = account.updatedAt
            )
        }
    }
}
