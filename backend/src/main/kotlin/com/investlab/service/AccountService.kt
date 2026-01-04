package com.investlab.service

import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.Account
import com.investlab.model.AccountRequest
import com.investlab.model.AccountResponse
import com.investlab.model.TradeResponse
import com.investlab.repository.AccountRepository
import com.investlab.repository.TradeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val tradeRepository: TradeRepository
) {
    
    fun getAllAccounts(): List<AccountResponse> {
        return accountRepository.findAll().map { AccountResponse.from(it) }
    }
    
    fun getAccountById(id: Long): AccountResponse {
        val account = accountRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("账户不存在: $id") }
        return AccountResponse.from(account)
    }
    
    @Transactional
    fun createAccount(request: AccountRequest): AccountResponse {
        val account = Account(
            name = request.name,
            broker = request.broker,
            currency = request.currency,
            balanceManual = request.balanceManual
        )
        val saved = accountRepository.save(account)
        return AccountResponse.from(saved)
    }
    
    @Transactional
    fun updateAccount(id: Long, request: AccountRequest): AccountResponse {
        val account = accountRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("账户不存在: $id") }
        
        account.name = request.name
        account.broker = request.broker
        account.currency = request.currency
        account.balanceManual = request.balanceManual
        account.updatedAt = LocalDateTime.now()
        
        val updated = accountRepository.save(account)
        return AccountResponse.from(updated)
    }
    
    @Transactional
    fun deleteAccount(id: Long) {
        if (!accountRepository.existsById(id)) {
            throw ResourceNotFoundException("账户不存在: $id")
        }
        accountRepository.deleteById(id)
    }
    
    fun searchAccounts(name: String): List<AccountResponse> {
        return accountRepository.findByNameContaining(name)
            .map { AccountResponse.from(it) }
    }

    fun getTradesByAccount(accountId: Long): List<TradeResponse> {
        if (!accountRepository.existsById(accountId)) {
            throw com.investlab.exception.ResourceNotFoundException("账户不存在: $accountId")
        }
        return tradeRepository.findByAccountId(accountId).map { TradeResponse.from(it) }
    }
}
