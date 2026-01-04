package com.investlab.service

import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.Viewpoint
import com.investlab.model.ViewpointRequest
import com.investlab.model.ViewpointResponse
import com.investlab.repository.AssetRepository
import com.investlab.repository.StrategyRepository
import com.investlab.repository.ViewpointRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ViewpointService(
    private val viewpointRepository: ViewpointRepository,
    private val strategyRepository: StrategyRepository,
    private val assetRepository: AssetRepository
) {
    
    fun getAllViewpoints(): List<ViewpointResponse> {
        return viewpointRepository.findAll().map { ViewpointResponse.from(it) }
    }
    
    fun getViewpointById(id: Long): ViewpointResponse {
        val viewpoint = viewpointRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("观点不存在: $id") }
        return ViewpointResponse.from(viewpoint)
    }
    
    @Transactional
    fun createViewpoint(request: ViewpointRequest): ViewpointResponse {
        // 验证策略是否存在
        if (!strategyRepository.existsById(request.strategyId)) {
            throw ResourceNotFoundException("策略不存在: ${request.strategyId}")
        }
        // 若带 assetId，校验标的存在
        request.assetId?.let {
            if (!assetRepository.existsById(it)) {
                throw ResourceNotFoundException("标的不存在: $it")
            }
        }
        
        val viewpoint = Viewpoint(
            strategyId = request.strategyId,
            assetId = request.assetId,
            title = request.title,
            summary = request.summary,
            link = request.link,
            remark = request.remark,
            tag = request.tag,
            viewpointDate = request.viewpointDate
        )
        val saved = viewpointRepository.save(viewpoint)
        return ViewpointResponse.from(saved)
    }
    
    @Transactional
    fun updateViewpoint(id: Long, request: ViewpointRequest): ViewpointResponse {
        val viewpoint = viewpointRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("观点不存在: $id") }
        
        // 验证策略是否存在
        if (!strategyRepository.existsById(request.strategyId)) {
            throw ResourceNotFoundException("策略不存在: ${request.strategyId}")
        }
        // 若带 assetId，校验标的存在
        request.assetId?.let {
            if (!assetRepository.existsById(it)) {
                throw ResourceNotFoundException("标的不存在: $it")
            }
        }
        
        viewpoint.strategyId = request.strategyId
        viewpoint.assetId = request.assetId
        viewpoint.title = request.title
        viewpoint.summary = request.summary
        viewpoint.link = request.link
        viewpoint.remark = request.remark
        viewpoint.tag = request.tag
        viewpoint.viewpointDate = request.viewpointDate
        viewpoint.updatedAt = LocalDateTime.now()
        
        val updated = viewpointRepository.save(viewpoint)
        return ViewpointResponse.from(updated)
    }
    
    @Transactional
    fun deleteViewpoint(id: Long) {
        if (!viewpointRepository.existsById(id)) {
            throw ResourceNotFoundException("观点不存在: $id")
        }
        viewpointRepository.deleteById(id)
    }
    
    fun getViewpointsByStrategy(strategyId: Long): List<ViewpointResponse> {
        return viewpointRepository.findByStrategyIdOrderByDateDesc(strategyId)
            .map { ViewpointResponse.from(it) }
    }
    
    fun getViewpointsByTag(tag: String): List<ViewpointResponse> {
        return viewpointRepository.findByTag(tag)
            .map { ViewpointResponse.from(it) }
    }

    fun getViewpointsByAsset(assetId: Long): List<ViewpointResponse> {
        return viewpointRepository.findByAssetIdOrderByDateDesc(assetId)
            .map { ViewpointResponse.from(it) }
    }
}
