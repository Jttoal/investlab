package com.investlab.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.server.ResponseStatusException

// SPA 前端路由转发：非 API/监控/静态资源请求统一转发到 index.html，避免深链路直接访问报错
@Controller
class SpaController {

    @GetMapping("/{path:[^\\.]*}")
    fun forwardLevel1(request: HttpServletRequest): String {
        if (shouldBypass(request.requestURI)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return "forward:/index.html"
    }

    @GetMapping("/*/{path:[^\\.]*}")
    fun forwardLevel2(request: HttpServletRequest): String {
        if (shouldBypass(request.requestURI)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return "forward:/index.html"
    }

    @GetMapping("/*/*/{path:[^\\.]*}")
    fun forwardLevel3(request: HttpServletRequest): String {
        if (shouldBypass(request.requestURI)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return "forward:/index.html"
    }

    private fun shouldBypass(uri: String): Boolean {
        val skipPrefixes = listOf(
            "/api",
            "/actuator",
            "/v3",
            "/swagger-ui",
            "/assets",
            "/static",
            "/webjars"
        )
        if (skipPrefixes.any { uri.startsWith(it) }) return true
        val skipExact = setOf("/favicon.ico", "/index.html")
        if (skipExact.contains(uri)) return true
        return false
    }
}
