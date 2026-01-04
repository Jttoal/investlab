package com.investlab.service

import com.investlab.exception.ResourceNotFoundException
import com.investlab.model.Account
import com.investlab.model.AccountRequest
import com.investlab.repository.AccountRepository
import com.investlab.repository.TradeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*

class AccountServiceTest {
    
    private lateinit var accountRepository: AccountRepository
    private lateinit var tradeRepository: TradeRepository
    private lateinit var accountService: AccountService
    
    @BeforeEach
    fun setup() {
        accountRepository = mockk()
        tradeRepository = mockk()
        accountService = AccountService(accountRepository, tradeRepository)
    }
    
    @Test
    fun `should get all accounts`() {
        val accounts = listOf(
            Account(id = 1, name = "账户1", broker = "券商1"),
            Account(id = 2, name = "账户2", broker = "券商2")
        )
        every { accountRepository.findAll() } returns accounts
        
        val result = accountService.getAllAccounts()
        
        assertEquals(2, result.size)
        verify { accountRepository.findAll() }
    }
    
    @Test
    fun `should get account by id`() {
        val account = Account(id = 1, name = "测试账户", broker = "测试券商")
        every { accountRepository.findById(1) } returns Optional.of(account)
        
        val result = accountService.getAccountById(1)
        
        assertEquals("测试账户", result.name)
        verify { accountRepository.findById(1) }
    }
    
    @Test
    fun `should throw exception when account not found`() {
        every { accountRepository.findById(999) } returns Optional.empty()
        
        assertThrows<ResourceNotFoundException> {
            accountService.getAccountById(999)
        }
    }
    
    @Test
    fun `should create account`() {
        val request = AccountRequest(
            name = "新账户",
            broker = "新券商",
            currency = "CNY",
            balanceManual = BigDecimal("10000.00")
        )
        val savedAccount = Account(
            id = 1,
            name = request.name,
            broker = request.broker,
            currency = request.currency,
            balanceManual = request.balanceManual
        )
        
        every { accountRepository.save(any()) } returns savedAccount
        
        val result = accountService.createAccount(request)
        
        assertEquals("新账户", result.name)
        assertEquals("新券商", result.broker)
        verify { accountRepository.save(any()) }
    }
    
    @Test
    fun `should update account`() {
        val existingAccount = Account(id = 1, name = "旧名称", broker = "旧券商")
        val request = AccountRequest(
            name = "新名称",
            broker = "新券商",
            currency = "USD",
            balanceManual = BigDecimal("20000.00")
        )
        
        every { accountRepository.findById(1) } returns Optional.of(existingAccount)
        every { accountRepository.save(any()) } returns existingAccount.apply {
            name = request.name
            broker = request.broker
        }
        
        val result = accountService.updateAccount(1, request)
        
        assertEquals("新名称", result.name)
        verify { accountRepository.findById(1) }
        verify { accountRepository.save(any()) }
    }
    
    @Test
    fun `should delete account`() {
        every { accountRepository.existsById(1) } returns true
        every { accountRepository.deleteById(1) } returns Unit
        
        accountService.deleteAccount(1)
        
        verify { accountRepository.existsById(1) }
        verify { accountRepository.deleteById(1) }
    }
}
