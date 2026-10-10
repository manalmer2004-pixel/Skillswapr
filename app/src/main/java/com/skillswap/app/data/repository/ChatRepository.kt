package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.skillswap.app.data.model.Chat
import com.skillswap.app.data.model.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getChatsFlow(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.documents?.mapNotNull { it.toObject(Chat::class.java) } ?: emptyList()
                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    fun getMessagesFlow(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String): Boolean {
        return try {
            val msgRef = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document()

            val message = ChatMessage(
                id = msgRef.id,
                senderId = senderId,
                text = text,
                sentAt = System.currentTimeMillis()
            )

            firestore.runBatch { batch ->
                batch.set(msgRef, message)
                batch.update(
                    firestore.collection("chats").document(chatId),
                    mapOf(
                        "lastMessage" to text,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createOrGetChat(participantIds: List<String>): String {
        val query = firestore.collection("chats")
            .whereArrayContains("participantIds", participantIds.first())
            .get()
            .await()

        val existingChat = query.documents.mapNotNull { it.toObject(Chat::class.java) }
            .firstOrNull { it.participantIds.containsAll(participantIds) }

        if (existingChat != null) return existingChat.id

        val newDoc = firestore.collection("chats").document()
        val newChat = Chat(
            id = newDoc.id,
            participantIds = participantIds,
            lastMessage = "Started a conversation",
            updatedAt = System.currentTimeMillis()
        )
        newDoc.set(newChat).await()
        return newDoc.id
    }
}
