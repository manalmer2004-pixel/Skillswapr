package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

enum class TransactionType {
    EARNED,
    SPENT,
    BONUS
}

data class CreditTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val type: TransactionType = TransactionType.BONUS,
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
