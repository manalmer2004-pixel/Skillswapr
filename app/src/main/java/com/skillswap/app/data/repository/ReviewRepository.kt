package com.skillswap.app.data.repository

import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.Review
import kotlinx.coroutines.tasks.await

class ReviewRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addReview(review: Review): Boolean {
        return try {
            val docId = "${review.sessionId}_${review.reviewerId}"
            val reviewRef = firestore.collection("reviews").document(docId)
            val reviewWithId = review.copy(id = docId)
            reviewRef.set(reviewWithId).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRatingSummary(userId: String): Pair<Double, Long> {
        return try {
            val query = firestore.collection("reviews")
                .whereEqualTo("revieweeId", userId)

            val snapshot = query.aggregate(
                AggregateField.count(),
                AggregateField.average("rating")
            ).get(AggregateSource.SERVER).await()

            val count = snapshot.get(AggregateField.count())
            val avg = snapshot.get(AggregateField.average("rating")) ?: 0.0
            Pair(avg, count)
        } catch (e: Exception) {
            Pair(0.0, 0L)
        }
    }
}
