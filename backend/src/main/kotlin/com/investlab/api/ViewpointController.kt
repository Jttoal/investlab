package com.investlab.api

import com.investlab.model.ViewpointRequest
import com.investlab.model.ViewpointResponse
import com.investlab.service.ViewpointService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/viewpoints")
class ViewpointController(
    private val viewpointService: ViewpointService
) {
    
    @GetMapping
    fun getAllViewpoints(
        @RequestParam(required = false) strategyId: Long?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false) assetId: Long?
    ): ResponseEntity<List<ViewpointResponse>> {
        val viewpoints = when {
            assetId != null -> viewpointService.getViewpointsByAsset(assetId)
            strategyId != null -> viewpointService.getViewpointsByStrategy(strategyId)
            tag != null -> viewpointService.getViewpointsByTag(tag)
            else -> viewpointService.getAllViewpoints()
        }
        return ResponseEntity.ok(viewpoints)
    }
    
    @GetMapping("/{id}")
    fun getViewpointById(@PathVariable id: Long): ResponseEntity<ViewpointResponse> {
        return ResponseEntity.ok(viewpointService.getViewpointById(id))
    }
    
    @PostMapping
    fun createViewpoint(@RequestBody request: ViewpointRequest): ResponseEntity<ViewpointResponse> {
        val created = viewpointService.createViewpoint(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
    
    @PutMapping("/{id}")
    fun updateViewpoint(
        @PathVariable id: Long,
        @RequestBody request: ViewpointRequest
    ): ResponseEntity<ViewpointResponse> {
        return ResponseEntity.ok(viewpointService.updateViewpoint(id, request))
    }
    
    @DeleteMapping("/{id}")
    fun deleteViewpoint(@PathVariable id: Long): ResponseEntity<Void> {
        viewpointService.deleteViewpoint(id)
        return ResponseEntity.noContent().build()
    }
}
