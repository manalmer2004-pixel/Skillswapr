package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

enum class SwapRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    COMPLETED,
    CANCELLED
}

data class SwapRequest(
    @DocumentId val id: String = "",
    val requesterId: String = "",
    val providerId: String = "",
    val skillId: String = "",
    val providerName: String = "",
    val skillTitle: String = "",
    val status: SwapRequestStatus = SwapRequestStatus.PENDING,
    val proposedTime: String = "",
    val durationHours: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis()
)
