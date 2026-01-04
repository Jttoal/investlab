package com.investlab.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.Setting
import com.investlab.model.SettingRequest
import com.investlab.model.SettingResponse
import com.investlab.model.SettingValueType
import com.investlab.repository.SettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SettingService(
    private val settingRepository: SettingRepository,
    private val objectMapper: ObjectMapper
) {

    fun list(keyword: String?): List<SettingResponse> {
        val list = if (keyword.isNullOrBlank()) {
            settingRepository.findAll()
        } else {
            settingRepository.searchByKey(keyword)
        }
        return list.map { Setting.toResponse(it) }
    }

    fun get(id: Long): SettingResponse {
        val setting = settingRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("设置不存在: $id") }
        return Setting.toResponse(setting)
    }

    @Transactional
    fun create(request: SettingRequest): SettingResponse {
        validate(request)
        val entity = Setting(
            key = request.key,
            value = normalizeValue(request),
            valueType = request.valueType,
            description = request.description
        )
        return Setting.toResponse(settingRepository.save(entity))
    }

    @Transactional
    fun update(id: Long, request: SettingRequest): SettingResponse {
        validate(request)
        val setting = settingRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("设置不存在: $id") }
        setting.key = request.key
        setting.value = normalizeValue(request)
        setting.valueType = request.valueType
        setting.description = request.description
        setting.updatedAt = LocalDateTime.now()
        return Setting.toResponse(settingRepository.save(setting))
    }

    @Transactional
    fun delete(id: Long) {
        if (!settingRepository.existsById(id)) {
            throw ResourceNotFoundException("设置不存在: $id")
        }
        settingRepository.deleteById(id)
    }

    fun getByKey(key: String): SettingResponse? {
        return settingRepository.findByKey(key)?.let { Setting.toResponse(it) }
    }

    private fun validate(request: SettingRequest) {
        if (request.key.isBlank()) throw IllegalArgumentException("key 不能为空")
        if (request.key.length > 150) throw IllegalArgumentException("key 过长")
        if (request.valueType == SettingValueType.json) {
            try {
                objectMapper.readTree(request.value)
            } catch (_: Exception) {
                throw IllegalArgumentException("JSON 格式无效")
            }
        }
    }

    private fun normalizeValue(request: SettingRequest): String {
        return if (request.valueType == SettingValueType.json) {
            val node = objectMapper.readTree(request.value)
            objectMapper.writer().with(SerializationFeature.INDENT_OUTPUT).writeValueAsString(node)
        } else {
            request.value
        }
    }
}
