package com.skillswap.app.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: String = "",
    val isSystem: Boolean = false
)
