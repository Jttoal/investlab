package com.investlab.api

import com.investlab.model.SettingRequest
import com.investlab.model.SettingResponse
import com.investlab.service.SettingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/settings")
class SettingController(
    private val settingService: SettingService
) {

    @GetMapping
    fun list(@RequestParam(required = false) keyword: String?): ResponseEntity<List<SettingResponse>> {
        return ResponseEntity.ok(settingService.list(keyword))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<SettingResponse> {
        return ResponseEntity.ok(settingService.get(id))
    }

    @PostMapping
    fun create(@RequestBody request: SettingRequest): ResponseEntity<SettingResponse> {
        val created = settingService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: SettingRequest): ResponseEntity<SettingResponse> {
        return ResponseEntity.ok(settingService.update(id, request))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        settingService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
