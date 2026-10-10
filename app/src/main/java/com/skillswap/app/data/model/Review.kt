package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

data class Review(
    @DocumentId val id: String = "",
    val sessionId: String = "",
    val reviewerId: String = "",
    val revieweeId: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
