package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.UserFavorite
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FavoriteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getFavoritesFlow(userId: String): Flow<List<UserFavorite>> = callbackFlow {
        val listener = firestore.collection("favorites")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val favorites = snapshot?.documents?.mapNotNull { it.toObject(UserFavorite::class.java) } ?: emptyList()
                trySend(favorites)
            }

        awaitClose { listener.remove() }
    }

    suspend fun toggleFavorite(userId: String, skillId: String): Boolean {
        return try {
            val docId = "${userId}_${skillId}"
            val docRef = firestore.collection("favorites").document(docId)
            val docSnap = docRef.get().await()

            if (docSnap.exists()) {
                docRef.delete().await()
            } else {
                val fav = UserFavorite(id = docId, userId = userId, skillId = skillId)
                docRef.set(fav).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
