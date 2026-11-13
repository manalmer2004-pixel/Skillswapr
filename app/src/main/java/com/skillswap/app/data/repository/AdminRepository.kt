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

    // --- PHASE 3 METHODS ---

    suspend fun updateSkillHidden(skillId: String, isHidden: Boolean): Result<Unit> {
        return try {
            firestore.collection("skills").document(skillId)
                .update("isHidden", isHidden).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSkill(skillId: String): Result<Unit> {
        return try {
            firestore.collection("skills").document(skillId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            firestore.collection("reviews").document(reviewId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCategory(name: String, order: Int): Result<Unit> {
        if (name.isBlank()) return Result.failure(Exception("Category name cannot be empty"))
        return try {
            val docRef = firestore.collection("categories").document()
            val category = Category(id = docRef.id, name = name.trim(), icon = "", order = order)
            docRef.set(category).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(categoryId: String, newName: String, newOrder: Int): Result<Unit> {
        if (newName.isBlank()) return Result.failure(Exception("Category name cannot be empty"))
        return try {
            firestore.collection("categories").document(categoryId)
                .update(mapOf("name" to newName.trim(), "order" to newOrder)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String, categoryName: String): Result<Unit> {
        return try {
            // Check if any skill uses this category name
            val skillsWithCategory = firestore.collection("skills")
                .whereEqualTo("category", categoryName)
                .limit(1)
                .get()
                .await()

            if (!skillsWithCategory.isEmpty) {
                return Result.failure(Exception("Cannot delete category: Skills are currently listed under '$categoryName'"))
            }

            firestore.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forceCancelSwapRequest(requestId: String, adminId: String, reason: String): Result<Unit> {
        if (reason.isBlank()) return Result.failure(Exception("Reason is required to force cancel a swap request"))
        return try {
            val reqSnap = firestore.collection("swapRequests").document(requestId).get().await()
            val request = reqSnap.toObject(SwapRequest::class.java)
                ?: return Result.failure(Exception("Swap request not found"))

            if (request.status != SwapRequestStatus.PENDING && request.status != SwapRequestStatus.ACCEPTED) {
                return Result.failure(Exception("Only PENDING or ACCEPTED swap requests can be cancelled"))
            }

            firestore.collection("swapRequests").document(requestId)
                .update("status", SwapRequestStatus.CANCELLED.name).await()

            // Notify requester
            if (request.requesterId.isNotBlank() && request.requesterId != adminId) {
                val notif1 = NotificationItem(
                    id = firestore.collection("notifications").document().id,
                    userId = request.requesterId,
                    type = NotificationType.ADMIN_ACTION,
                    refId = requestId,
                    message = "Swap request for '${request.skillTitle}' was cancelled by an admin. Reason: $reason",
                    createdAt = System.currentTimeMillis()
                )
                firestore.collection("notifications").add(notif1).await()
            }

            // Notify provider
            if (request.providerId.isNotBlank() && request.providerId != adminId && request.providerId != request.requesterId) {
                val notif2 = NotificationItem(
                    id = firestore.collection("notifications").document().id,
                    userId = request.providerId,
                    type = NotificationType.ADMIN_ACTION,
                    refId = requestId,
                    message = "Swap request for '${request.skillTitle}' was cancelled by an admin. Reason: $reason",
                    createdAt = System.currentTimeMillis()
                )
                firestore.collection("notifications").add(notif2).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun broadcastAnnouncement(adminId: String, title: String, message: String): Result<Int> {
        if (title.isBlank() || message.isBlank()) return Result.failure(Exception("Title and message are required for broadcast"))
        return try {
            val users = getAllUsers().filter { it.id != adminId && it.id.isNotBlank() }
            if (users.isEmpty()) return Result.success(0)

            val fullText = "📢 $title: $message"
            val chunks = users.chunked(450) // Firestore batch limit is 500
            var count = 0

            for (chunk in chunks) {
                val batch = firestore.batch()
                for (user in chunk) {
                    val docRef = firestore.collection("notifications").document()
                    val notif = NotificationItem(
                        id = docRef.id,
                        userId = user.id,
                        type = NotificationType.ADMIN_ANNOUNCEMENT,
                        refId = "broadcast_${System.currentTimeMillis()}",
                        message = fullText,
                        createdAt = System.currentTimeMillis()
                    )
                    batch.set(docRef, notif)
                    count++
                }
                batch.commit().await()
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
