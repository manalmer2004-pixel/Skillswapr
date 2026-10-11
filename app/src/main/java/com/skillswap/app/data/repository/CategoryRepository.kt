package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.skillswap.app.data.model.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CategoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val defaultCategories = listOf(
        "Water & Sanitation", "Coding", "Languages", "Music",
        "Cooking", "Gardening", "Tutoring", "Crafts", "Fitness", "Other"
    )

    companion object {
        private var seedAttempted = false   // seed at most once per app run
    }

    fun getCategoriesFlow(): Flow<List<Category>> = callbackFlow {
        val listener = firestore.collection("categories")
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                // Ignore local, not-yet-confirmed snapshots, which caused the flicker
                if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener

                val categories = snapshot.documents.mapNotNull { it.toObject(Category::class.java) }

                // Only seed when the SERVER confirms the collection is empty, and only once
                if (categories.isEmpty() && !snapshot.metadata.isFromCache && !seedAttempted) {
                    seedAttempted = true
                    seedCategories()
                }
                trySend(categories)
            }

        awaitClose { listener.remove() }
    }

    private fun seedCategories() {
        val batch = firestore.batch()
        defaultCategories.forEachIndexed { index, name ->
            val docRef = firestore.collection("categories").document("cat_${index + 1}")
            batch.set(docRef, Category(id = docRef.id, name = name, icon = "", order = index + 1))
        }
        batch.commit()   // only succeeds for an admin user; others are rejected quietly
    }
}
