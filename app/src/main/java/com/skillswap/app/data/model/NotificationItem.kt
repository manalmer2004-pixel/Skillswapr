package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

enum class NotificationType {
    REQUEST_CREATED,
    REQUEST_ACCEPTED,
    REQUEST_DECLINED,
    REQUEST_COMPLETED
}

data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.REQUEST_CREATED,
    val refId: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
