package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.Review
import com.skillswap.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await

class ReviewRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addReview(review: Review): Boolean {
        return try {
            firestore.runTransaction { transaction ->
                val reviewRef = firestore.collection("reviews").document()
                val reviewWithId = review.copy(id = reviewRef.id)
                transaction.set(reviewRef, reviewWithId)

                val userRef = firestore.collection("users").document(review.revieweeId)
                val userSnap = transaction.get(userRef)
                val userProfile = userSnap.toObject(UserProfile::class.java)

                if (userProfile != null) {
                    val currentCount = userProfile.reviewCount
                    val currentRating = userProfile.rating
                    val newCount = currentCount + 1
                    val newRating = ((currentRating * currentCount) + review.rating) / newCount

                    transaction.update(userRef, mapOf(
                        "reviewCount" to newCount,
                        "rating" to newRating
                    ))
                }
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
