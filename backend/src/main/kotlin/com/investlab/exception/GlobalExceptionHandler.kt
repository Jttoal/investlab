package com.investlab.exception

import com.investlab.model.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        logger.error("Resource not found - traceId: $traceId", ex)
        
        val error = ErrorResponse(
            code = "RESOURCE_NOT_FOUND",
            message = ex.message ?: "资源不存在",
            traceId = traceId
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }
    
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        logger.error("Business exception - traceId: $traceId", ex)
        
        val error = ErrorResponse(
            code = "BUSINESS_ERROR",
            message = ex.message ?: "业务处理失败",
            traceId = traceId
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }
    
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(ex: ValidationException): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        logger.error("Validation exception - traceId: $traceId", ex)
        
        val error = ErrorResponse(
            code = "VALIDATION_ERROR",
            message = ex.message ?: "数据验证失败",
            traceId = traceId
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        logger.error("Unexpected error - traceId: $traceId", ex)
        
        val error = ErrorResponse(
            code = "INTERNAL_ERROR",
            message = "系统内部错误",
            traceId = traceId
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
