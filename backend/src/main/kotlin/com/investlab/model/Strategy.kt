package com.investlab.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "strategy")
data class Strategy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, length = 100)
    var name: String,
    
    @Column(nullable = false, length = 50)
    var type: String,
    
    @Column(name = "goal_note", columnDefinition = "TEXT")
    var goalNote: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

data class StrategyRequest(
    val name: String,
    val type: String,
    val goalNote: String? = null
)

data class StrategyResponse(
    val id: Long,
    val name: String,
    val type: String,
    val goalNote: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(strategy: Strategy): StrategyResponse {
            return StrategyResponse(
                id = strategy.id!!,
                name = strategy.name,
                type = strategy.type,
                goalNote = strategy.goalNote,
                createdAt = strategy.createdAt,
                updatedAt = strategy.updatedAt
            )
        }
    }
}
