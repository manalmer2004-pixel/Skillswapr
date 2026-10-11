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
        if (userId.isBlank()) return Pair(0.0, 0L)
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

    suspend fun getRatingSummaries(userIds: List<String>): Map<String, Pair<Double, Long>> {
        val cleanIds = userIds.distinct().filter { it.isNotBlank() }
        if (cleanIds.isEmpty()) return emptyMap()

        val resultMap = mutableMapOf<String, Pair<Double, Long>>()
        val chunks = cleanIds.chunked(10)

        for (chunk in chunks) {
            try {
                val snapshot = firestore.collection("reviews")
                    .whereIn("revieweeId", chunk)
                    .get()
                    .await()

                val reviews = snapshot.documents.mapNotNull { doc ->
                    try { doc.toObject(Review::class.java) } catch (_: Exception) { null }
                }

                val grouped = reviews.groupBy { it.revieweeId }
                for (id in chunk) {
                    val userReviews = grouped[id] ?: emptyList()
                    if (userReviews.isNotEmpty()) {
                        val count = userReviews.size.toLong()
                        val avg = userReviews.map { it.rating }.average()
                        resultMap[id] = Pair(avg, count)
                    } else {
                        resultMap[id] = Pair(0.0, 0L)
                    }
                }
            } catch (_: Exception) {
                for (id in chunk) {
                    resultMap[id] = Pair(0.0, 0L)
                }
            }
        }
        return resultMap
    }
}
