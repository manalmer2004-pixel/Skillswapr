package com.skillswap.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationType {
    REQUEST_CREATED,
    REQUEST_ACCEPTED,
    REQUEST_DECLINED,
    REQUEST_COMPLETED,
    REQUEST_CANCELLED,
    ADMIN_ANNOUNCEMENT,
    ADMIN_ACTION
}

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.REQUEST_CREATED,
    val refId: String = "",
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
