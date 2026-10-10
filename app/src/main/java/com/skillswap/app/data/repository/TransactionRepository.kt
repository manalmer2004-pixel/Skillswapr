package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.skillswap.app.data.model.CreditTransaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TransactionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getTransactionsFlow(userId: String): Flow<List<CreditTransaction>> = callbackFlow {
        val listener = firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents?.mapNotNull { it.toObject(CreditTransaction::class.java) } ?: emptyList()
                trySend(transactions)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addTransaction(transaction: CreditTransaction): Boolean {
        return try {
            val docRef = firestore.collection("transactions").document()
            val tx = transaction.copy(id = docRef.id)
            docRef.set(tx).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
