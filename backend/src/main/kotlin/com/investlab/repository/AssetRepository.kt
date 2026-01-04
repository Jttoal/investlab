package com.investlab.repository

import com.investlab.model.Asset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssetRepository : JpaRepository<Asset, Long> {
    fun findByStrategyId(strategyId: Long): List<Asset>
    fun findBySymbol(symbol: String): List<Asset>
}
