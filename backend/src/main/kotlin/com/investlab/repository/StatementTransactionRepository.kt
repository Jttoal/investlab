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
          AND (:keyword IS NULL OR LOWER(COALESCE(t.txnTypeRaw, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:counterparty IS NULL OR LOWER(COALESCE(t.counterparty, '')) LIKE LOWER(CONCAT('%', :counterparty, '%')))
          AND (:accountName IS NULL OR LOWER(COALESCE(t.accountName, '')) LIKE LOWER(CONCAT('%', :accountName, '%')))
          AND (:amountDirection IS NULL OR 
               (:amountDirection = 'positive' AND t.amount > 0) OR 
               (:amountDirection = 'negative' AND t.amount < 0))
        ORDER BY t.txnDate DESC, t.id DESC
        """
    )
    fun search(
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        @Param("category") category: TransactionCategory?,
        @Param("direction") direction: TransactionDirection?,
        @Param("keyword") keyword: String?,
        @Param("accountName") accountName: String?,
        @Param("counterparty") counterparty: String?,
        @Param("amountDirection") amountDirection: String?
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

    // 按交易摘要汇总（支出）
    @Query(
        value = """
        SELECT txn_type_raw AS summary,
               CAST(SUM(ABS(amount)) AS REAL) AS total
        FROM statement_transaction
        WHERE category = :category
          AND direction IN (:directions)
          AND date(txn_date/1000, 'unixepoch', 'localtime') >= date(:startDate)
          AND date(txn_date/1000, 'unixepoch', 'localtime') < date(:endDate)
          AND (:accountNames IS NULL OR account_name IN (:accountNames))
        GROUP BY txn_type_raw
        ORDER BY total DESC
        """,
        nativeQuery = true
    )
    fun summarizeByTypeRaw(
        @Param("category") category: String,
        @Param("directions") directions: List<String>,
        @Param("startDate") startDate: String,
        @Param("endDate") endDate: String,
        @Param("accountNames") accountNames: List<String>?
    ): List<Array<Any>>

    // 月度/年度趋势
    @Query(
        value = """
        SELECT strftime(:format, txn_date/1000, 'unixepoch', 'localtime') AS period,
               SUM(CASE WHEN direction IN (:incomeDirections) THEN amount ELSE 0 END) AS income,
               SUM(CASE WHEN direction IN (:expenseDirections) THEN ABS(amount) ELSE 0 END) AS expense,
               SUM(amount) AS balance
        FROM statement_transaction
        WHERE category = :category
          AND date(txn_date/1000, 'unixepoch', 'localtime') >= date(:startDate)
          AND date(txn_date/1000, 'unixepoch', 'localtime') < date(:endDate)
          AND (:accountNames IS NULL OR account_name IN (:accountNames))
        GROUP BY period
        ORDER BY period ASC
        """,
        nativeQuery = true
    )
    fun getTrendData(
        @Param("format") format: String,  // '%Y-%m' 或 '%Y'
        @Param("category") category: String,
        @Param("incomeDirections") incomeDirections: List<String>,
        @Param("expenseDirections") expenseDirections: List<String>,
        @Param("startDate") startDate: String,
        @Param("endDate") endDate: String,
        @Param("accountNames") accountNames: List<String>?
    ): List<Array<Any>>
}
