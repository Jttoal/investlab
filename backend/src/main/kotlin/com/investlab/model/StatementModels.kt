package com.investlab.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class TransactionCategory { ordinary, investment }

enum class TransactionDirection { expense, income, buy, redeem }

@Entity
@Table(name = "statement_file")
data class StatementFile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String,

    @Column(name = "file_md5", length = 64)
    var fileMd5: String? = null,

    @Column(name = "account_name", columnDefinition = "TEXT")
    var accountName: String? = null,

    @Column(name = "file_path", columnDefinition = "TEXT")
    var filePath: String? = null,

    @Column(name = "period_start")
    var periodStart: LocalDate? = null,

    @Column(name = "period_end")
    var periodEnd: LocalDate? = null,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: StatementFileStatus = StatementFileStatus.pending,

    @Column(name = "total_rows", nullable = false)
    var totalRows: Int = 0,

    @Column(name = "inserted_rows", nullable = false)
    var insertedRows: Int = 0,

    @Column(name = "dedup_rows", nullable = false)
    var dedupRows: Int = 0,

    @Column(name = "failed_rows", nullable = false)
    var failedRows: Int = 0,

    @Column(name = "error_msg", columnDefinition = "TEXT")
    var errorMsg: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null
)

enum class StatementFileStatus { pending, processing, success, failed }

@Entity
@Table(
    name = "statement_transaction",
    indexes = [
        Index(name = "idx_statement_transaction_date", columnList = "txn_date"),
        Index(name = "idx_statement_transaction_category_direction", columnList = "category, direction"),
        Index(name = "idx_statement_transaction_account_name", columnList = "account_name")
    ]
)
data class StatementTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "file_id", nullable = false)
    var fileId: Long,

    @Column(name = "txn_date", nullable = false)
    var txnDate: LocalDate,

    @Column(name = "currency", nullable = false, length = 10)
    var currency: String,

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    var amount: BigDecimal,

    @Column(name = "balance", precision = 15, scale = 2)
    var balance: BigDecimal? = null,

    @Column(name = "txn_type_raw", columnDefinition = "TEXT")
    var txnTypeRaw: String? = null,

    @Column(name = "counterparty", columnDefinition = "TEXT")
    var counterparty: String? = null,

    @Column(name = "account_name", columnDefinition = "TEXT")
    var accountName: String? = null,

    @Column(name = "category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var category: TransactionCategory,

    @Column(name = "direction", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var direction: TransactionDirection,

    @Column(name = "dedup_key", nullable = false, length = 120, unique = true)
    var dedupKey: String,

    @Column(name = "hash_raw", columnDefinition = "TEXT")
    var hashRaw: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class StatementFileResponse(
    val id: Long,
    val userId: Long,
    val fileName: String,
    val accountName: String?,
    val status: StatementFileStatus,
    val totalRows: Int,
    val insertedRows: Int,
    val dedupRows: Int,
    val failedRows: Int,
    val errorMsg: String?,
    val createdAt: LocalDateTime,
    val finishedAt: LocalDateTime?
) {
    companion object {
        fun from(entity: StatementFile): StatementFileResponse =
            StatementFileResponse(
                id = entity.id!!,
                userId = entity.userId,
                fileName = entity.fileName,
                accountName = entity.accountName,
                status = entity.status,
                totalRows = entity.totalRows,
                insertedRows = entity.insertedRows,
                dedupRows = entity.dedupRows,
                failedRows = entity.failedRows,
                errorMsg = entity.errorMsg,
                createdAt = entity.createdAt,
                finishedAt = entity.finishedAt
            )
    }
}

data class StatementTransactionResponse(
    val id: Long,
    val fileId: Long,
    val txnDate: LocalDate,
    val currency: String,
    val amount: BigDecimal,
    val balance: BigDecimal?,
    val txnTypeRaw: String?,
    val counterparty: String?,
    val accountName: String?,
    val category: TransactionCategory,
    val direction: TransactionDirection,
    val dedupKey: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: StatementTransaction): StatementTransactionResponse =
            StatementTransactionResponse(
                id = entity.id!!,
                fileId = entity.fileId,
                txnDate = entity.txnDate,
                currency = entity.currency,
                amount = entity.amount,
                balance = entity.balance,
                txnTypeRaw = entity.txnTypeRaw,
                counterparty = entity.counterparty,
                accountName = entity.accountName,
                category = entity.category,
                direction = entity.direction,
                dedupKey = entity.dedupKey,
                createdAt = entity.createdAt
            )
    }
}

data class StatementSummaryResponse(
    val month: String,
    val category: TransactionCategory,
    val direction: TransactionDirection,
    val totalAmount: BigDecimal
)
