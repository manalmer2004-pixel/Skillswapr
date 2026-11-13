package com.skillswap.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EARNED,
    SPENT,
    BONUS,
    ADMIN_ADJUSTMENT
}

@Entity(tableName = "credit_transactions")
data class CreditTransaction(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val type: TransactionType = TransactionType.BONUS,
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val reason: String = "",
    val adminId: String = ""
)
