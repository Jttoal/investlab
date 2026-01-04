package com.investlab.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)

class BusinessException(message: String) : RuntimeException(message)

class ValidationException(message: String) : RuntimeException(message)
