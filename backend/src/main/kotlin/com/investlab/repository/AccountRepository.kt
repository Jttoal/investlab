package com.investlab.repository

import com.investlab.model.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    fun findByNameContaining(name: String): List<Account>
}
