package com.investlab.api

import com.investlab.model.AccountRequest
import com.investlab.model.AccountResponse
import com.investlab.service.AccountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountService: AccountService
) {
    
    @GetMapping
    fun getAllAccounts(): ResponseEntity<List<AccountResponse>> {
        return ResponseEntity.ok(accountService.getAllAccounts())
    }
    
    @GetMapping("/{id}")
    fun getAccountById(@PathVariable id: Long): ResponseEntity<AccountResponse> {
        return ResponseEntity.ok(accountService.getAccountById(id))
    }
    
    @PostMapping
    fun createAccount(@RequestBody request: AccountRequest): ResponseEntity<AccountResponse> {
        val created = accountService.createAccount(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
    
    @PutMapping("/{id}")
    fun updateAccount(
        @PathVariable id: Long,
        @RequestBody request: AccountRequest
    ): ResponseEntity<AccountResponse> {
        return ResponseEntity.ok(accountService.updateAccount(id, request))
    }
    
    @DeleteMapping("/{id}")
    fun deleteAccount(@PathVariable id: Long): ResponseEntity<Void> {
        accountService.deleteAccount(id)
        return ResponseEntity.noContent().build()
    }
    
    @GetMapping("/search")
    fun searchAccounts(@RequestParam name: String): ResponseEntity<List<AccountResponse>> {
        return ResponseEntity.ok(accountService.searchAccounts(name))
    }

    @GetMapping("/{id}/trades")
    fun getAccountTrades(@PathVariable id: Long): ResponseEntity<List<com.investlab.model.TradeResponse>> {
        return ResponseEntity.ok(accountService.getTradesByAccount(id))
    }
}
