package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.UserAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AvailabilityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getAvailabilityFlow(userId: String): Flow<List<UserAvailability>> = callbackFlow {
        val listener = firestore.collection("availability")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val availabilities = snapshot?.documents?.mapNotNull { it.toObject(UserAvailability::class.java) } ?: emptyList()
                trySend(availabilities)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveAvailability(availability: UserAvailability): Boolean {
        return try {
            val docRef = if (availability.id.isBlank()) {
                firestore.collection("availability").document()
            } else {
                firestore.collection("availability").document(availability.id)
            }
            docRef.set(availability.copy(id = docRef.id)).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
