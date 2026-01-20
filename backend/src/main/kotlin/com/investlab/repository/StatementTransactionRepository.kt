package com.investlab.repository

import com.investlab.model.StatementTransaction
import com.investlab.model.TransactionCategory
import com.investlab.model.TransactionDirection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface StatementTransactionRepository : JpaRepository<StatementTransaction, Long> {
    fun existsByDedupKey(dedupKey: String): Boolean
    fun findByFileId(fileId: Long): List<StatementTransaction>

    @Query(
        """
        SELECT t FROM StatementTransaction t
        WHERE (:startDate IS NULL OR t.txnDate >= :startDate)
          AND (:endDate IS NULL OR t.txnDate <= :endDate)
          AND (:category IS NULL OR t.category = :category)
          AND (:direction IS NULL OR t.direction = :direction)
          AND (:keyword IS NULL OR LOWER(COALESCE(t.txnTypeRaw, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(t.counterparty, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:accountName IS NULL OR LOWER(COALESCE(t.accountName, '')) LIKE LOWER(CONCAT('%', :accountName, '%')))
        ORDER BY t.txnDate DESC, t.id DESC
        """
    )
    fun search(
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        @Param("category") category: TransactionCategory?,
        @Param("direction") direction: TransactionDirection?,
        @Param("keyword") keyword: String?,
        @Param("accountName") accountName: String?
    ): List<StatementTransaction>

    @Query(
        value = """
        SELECT strftime('%Y-%m', txn_date) AS month,
               category AS category,
               direction AS direction,
               SUM(amount) AS totalAmount
        FROM statement_transaction
        WHERE (:startDate IS NULL OR txn_date >= :startDate)
          AND (:endDate IS NULL OR txn_date <= :endDate)
        GROUP BY month, category, direction
        ORDER BY month ASC
        """,
        nativeQuery = true
    )
    fun summarizeByMonth(
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?
    ): List<Array<Any>>
}
