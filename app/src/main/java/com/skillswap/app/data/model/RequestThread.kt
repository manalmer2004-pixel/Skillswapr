package com.skillswap.app.data.model

data class RequestThread(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingCategory: String = "Water & Sanitation",
    val otherUser: UserProfile = UserProfile(),
    val status: String = "pending", // "pending", "accepted", "completed", "declined"
    val creditsProposed: Double = 1.0,
    val proposedDate: String = "Saturday, 10:00 AM",
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val unreadCount: Int = 0,
    val messages: List<ChatMessage> = emptyList()
)
