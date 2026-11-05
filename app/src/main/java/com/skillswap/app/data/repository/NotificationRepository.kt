package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.NotificationItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getNotificationsFlow(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifs = snapshot?.documents
                    ?.mapNotNull { it.toObject(NotificationItem::class.java) }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(notifs)
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String): Boolean {
        if (notificationId.isBlank()) return false
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("read", true)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
