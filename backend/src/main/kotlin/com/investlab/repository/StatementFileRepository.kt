package com.investlab.repository

import com.investlab.model.StatementFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StatementFileRepository : JpaRepository<StatementFile, Long> {
    fun findByFileMd5(fileMd5: String): StatementFile?
}
