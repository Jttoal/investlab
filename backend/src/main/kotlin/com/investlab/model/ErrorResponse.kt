package com.investlab.model

import java.time.LocalDateTime

data class ErrorResponse(
    val code: String,
    val message: String,
    val traceId: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
