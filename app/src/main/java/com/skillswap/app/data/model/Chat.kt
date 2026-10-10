package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

data class Chat(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
