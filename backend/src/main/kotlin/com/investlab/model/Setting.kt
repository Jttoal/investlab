package com.investlab.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "setting")
data class Setting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "config_key", nullable = false, unique = true, length = 150)
    var key: String,

    @Column(name = "config_value", columnDefinition = "TEXT", nullable = false)
    var value: String,

    @Column(name = "value_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var valueType: SettingValueType = SettingValueType.string,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun toResponse(setting: Setting): SettingResponse {
            return SettingResponse(
                id = setting.id!!,
                key = setting.key,
                value = setting.value,
                valueType = setting.valueType,
                description = setting.description,
                createdAt = setting.createdAt,
                updatedAt = setting.updatedAt
            )
        }
    }
}
