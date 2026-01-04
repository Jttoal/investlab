package com.investlab.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "viewpoint")
data class Viewpoint(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "strategy_id", nullable = false)
    var strategyId: Long,

    @Column(name = "asset_id")
    var assetId: Long? = null,
    
    @Column(nullable = false, length = 200)
    var title: String,
    
    @Column(columnDefinition = "TEXT")
    var summary: String? = null,
    
    @Column(length = 500)
    var link: String? = null,

    @Column(columnDefinition = "TEXT")
    var remark: String? = null,
    
    @Column(length = 50)
    var tag: String? = null,
    
    @Column(name = "viewpoint_date", nullable = false)
    var viewpointDate: LocalDate,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

data class ViewpointRequest(
    val strategyId: Long,
    val assetId: Long? = null,
    val title: String,
    val summary: String? = null,
    val link: String? = null,
    val remark: String? = null,
    val tag: String? = null,
    val viewpointDate: LocalDate
)

data class ViewpointResponse(
    val id: Long,
    val strategyId: Long,
    val assetId: Long?,
    val title: String,
    val summary: String?,
    val link: String?,
    val remark: String?,
    val tag: String?,
    val viewpointDate: LocalDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(viewpoint: Viewpoint): ViewpointResponse {
            return ViewpointResponse(
                id = viewpoint.id!!,
                strategyId = viewpoint.strategyId,
                assetId = viewpoint.assetId,
                title = viewpoint.title,
                summary = viewpoint.summary,
                link = viewpoint.link,
                remark = viewpoint.remark,
                tag = viewpoint.tag,
                viewpointDate = viewpoint.viewpointDate,
                createdAt = viewpoint.createdAt,
                updatedAt = viewpoint.updatedAt
            )
        }
    }
}
