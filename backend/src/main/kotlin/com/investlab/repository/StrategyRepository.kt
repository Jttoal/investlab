package com.investlab.repository

import com.investlab.model.Strategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StrategyRepository : JpaRepository<Strategy, Long> {
    fun findByType(type: String): List<Strategy>
    fun findByNameContaining(name: String): List<Strategy>
}
