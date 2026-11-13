package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getAllUsers(): List<UserProfile> {
        return try {
            val snapshot = firestore.collection("users").limit(300).get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(UserProfile::class.java) } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllSkills(): List<SkillListing> {
        return try {
            val snapshot = firestore.collection("skills").get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(SkillListing::class.java) } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllSwapRequests(): List<SwapRequest> {
        return try {
            val snapshot = firestore.collection("swapRequests").get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(SwapRequest::class.java) } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllTransactions(): List<CreditTransaction> {
        return try {
            val snapshot = firestore.collection("transactions").get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(CreditTransaction::class.java) } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllReviews(): List<Review> {
        return try {
            val snapshot = firestore.collection("reviews").get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(Review::class.java) } catch (_: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserTransactions(userId: String): List<CreditTransaction> {
        return try {
            val snapshot = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(CreditTransaction::class.java) } catch (_: Exception) { null }
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateUserSuspension(
        targetUserId: String,
        adminId: String,
        isSuspended: Boolean
    ): Result<Unit> {
        if (targetUserId == adminId) return Result.failure(Exception("You cannot suspend your own account"))
        return try {
            firestore.collection("users").document(targetUserId)
                .update("isSuspended", isSuspended).await()

            val notifRef = firestore.collection("notifications").document()
            val msg = if (isSuspended) "Your account has been suspended by an administrator."
            else "Your account suspension has been lifted by an administrator."
            val notif = NotificationItem(
                id = notifRef.id,
                userId = targetUserId,
                type = NotificationType.ADMIN_ACTION,
                refId = targetUserId,
                message = msg,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("notifications").add(notif).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserVerification(
        targetUserId: String,
        adminId: String,
        isVerified: Boolean
    ): Result<Unit> {
        return try {
            firestore.collection("users").document(targetUserId)
                .update("isVerified", isVerified).await()

            val notifRef = firestore.collection("notifications").document()
            val msg = if (isVerified) "Congratulations! Your account has been verified."
            else "Your account verification status was updated by an administrator."
            val notif = NotificationItem(
                id = notifRef.id,
                userId = targetUserId,
                type = NotificationType.ADMIN_ACTION,
                refId = targetUserId,
                message = msg,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("notifications").add(notif).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adjustUserCredits(
        targetUserId: String,
        adminId: String,
        amount: Double,
        reason: String
    ): Result<Double> {
        if (targetUserId.isBlank() || adminId.isBlank()) return Result.failure(Exception("Invalid user ID"))
        if (targetUserId == adminId) return Result.failure(Exception("You cannot adjust your own credit balance"))
        if (amount == 0.0) return Result.failure(Exception("Adjustment amount must be non-zero"))
        if (reason.isBlank()) return Result.failure(Exception("Reason is required for credit adjustment"))

        val uuid = UUID.randomUUID().toString()
        val adjTxId = "adj_$uuid"

        return try {
            val newBalance = firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(targetUserId)
                val userSnap = transaction.get(userRef)
                val profile = userSnap.toObject(UserProfile::class.java)
                    ?: throw IllegalStateException("User profile not found")

                val currentBalance = profile.timeCreditBalance
                val updatedBalance = currentBalance + amount
                if (updatedBalance < 0.0) {
                    throw IllegalStateException("Credit balance cannot be negative (Current: $currentBalance, Adjustment: $amount)")
                }

                transaction.update(userRef, "timeCreditBalance", updatedBalance)

                val txRef = firestore.collection("transactions").document(adjTxId)
                val creditTx = CreditTransaction(
                    id = adjTxId,
                    userId = targetUserId,
                    sessionId = "admin_adj_$uuid",
                    type = TransactionType.ADMIN_ADJUSTMENT,
                    amount = amount,
                    balanceAfter = updatedBalance,
                    createdAt = System.currentTimeMillis(),
                    reason = reason,
                    adminId = adminId
                )
                transaction.set(txRef, creditTx)

                val notifRef = firestore.collection("notifications").document()
                val signStr = if (amount >= 0) "+$amount" else "$amount"
                val notif = NotificationItem(
                    id = notifRef.id,
                    userId = targetUserId,
                    type = NotificationType.ADMIN_ACTION,
                    refId = adjTxId,
                    message = "Admin credit adjustment: $signStr credits. Reason: $reason",
                    createdAt = System.currentTimeMillis()
                )
                transaction.set(notifRef, notif)

                updatedBalance
            }.await()

            Result.success(newBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
