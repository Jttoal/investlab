package com.investlab.repository

import com.investlab.model.Viewpoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ViewpointRepository : JpaRepository<Viewpoint, Long> {
    fun findByStrategyId(strategyId: Long): List<Viewpoint>
    fun findByTag(tag: String): List<Viewpoint>
    fun findByAssetId(assetId: Long): List<Viewpoint>
    
    @Query("SELECT v FROM Viewpoint v WHERE v.strategyId = :strategyId ORDER BY v.viewpointDate DESC")
    fun findByStrategyIdOrderByDateDesc(strategyId: Long): List<Viewpoint>

    @Query("SELECT v FROM Viewpoint v WHERE v.assetId = :assetId ORDER BY v.viewpointDate DESC")
    fun findByAssetIdOrderByDateDesc(assetId: Long): List<Viewpoint>
}
