package com.investlab.repository

import com.investlab.model.Setting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SettingRepository : JpaRepository<Setting, Long> {
    fun findByKey(key: String): Setting?

    @Query("SELECT s FROM Setting s WHERE s.key LIKE %:keyword% ORDER BY s.updatedAt DESC")
    fun searchByKey(keyword: String): List<Setting>
}
