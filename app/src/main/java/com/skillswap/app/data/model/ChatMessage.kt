package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: String = "Just now",
    val sentAt: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val read: Boolean = false
)
