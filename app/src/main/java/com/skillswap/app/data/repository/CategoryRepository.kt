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

    fun getCategoriesFlow(): Flow<List<Category>> = callbackFlow {
        val listener = firestore.collection("categories")
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { it.toObject(Category::class.java) } ?: emptyList()
                if (categories.isEmpty()) {
                    seedCategoriesAsync()
                }
                trySend(categories)
            }

        awaitClose { listener.remove() }
    }

    private fun seedCategoriesAsync() {
        val batch = firestore.batch()
        defaultCategories.forEachIndexed { index, name ->
            val docRef = firestore.collection("categories").document("cat_${index + 1}")
            val category = Category(id = docRef.id, name = name, icon = "", order = index + 1)
            batch.set(docRef, category)
        }
        batch.commit()
    }
}
