package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SwapRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun createSwapRequest(request: SwapRequest): Result<String> {
        return try {
            if (request.requesterId == request.providerId) {
                return Result.failure(Exception("Requester cannot be the provider for a swap request"))
            }

            val learnerDoc = firestore.collection("users").document(request.requesterId).get().await()
            val learnerProfile = learnerDoc.toObject(UserProfile::class.java)
            val balance = learnerProfile?.timeCreditBalance ?: 0.0

            if (balance < request.durationHours) {
                return Result.failure(Exception("Insufficient time credit balance for this request"))
            }

            val docRef = firestore.collection("swapRequests").document()
            val requestId = docRef.id
            val requestWithId = request.copy(id = requestId)
            docRef.set(requestWithId).await()

            val notification = NotificationItem(
                userId = request.providerId,
                type = NotificationType.REQUEST_CREATED,
                refId = requestId,
                message = "New swap request for '${request.skillTitle}'"
            )
            firestore.collection("notifications").add(notification).await()
            Result.success(requestId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSwapRequestsFlow(userId: String): Flow<List<SwapRequest>> = callbackFlow {
        val listener = firestore.collection("swapRequests")
            .whereEqualTo("requesterId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toObject(SwapRequest::class.java) } ?: emptyList()
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    fun getReceivedRequestsFlow(userId: String): Flow<List<SwapRequest>> = callbackFlow {
        val listener = firestore.collection("swapRequests")
            .whereEqualTo("providerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toObject(SwapRequest::class.java) } ?: emptyList()
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateStatus(requestId: String, newStatus: SwapRequestStatus, callerId: String): Result<Unit> {
        return try {
            if (newStatus == SwapRequestStatus.COMPLETED) {
                firestore.runTransaction { transaction ->
                    val requestRef = firestore.collection("swapRequests").document(requestId)
                    val requestSnap = transaction.get(requestRef)
                    val request = requestSnap.toObject(SwapRequest::class.java)
                        ?: throw IllegalStateException("Swap request not found")

                    if (request.status != SwapRequestStatus.ACCEPTED) {
                        throw IllegalStateException("Swap request can only be marked COMPLETED when in ACCEPTED status")
                    }

                    if (request.requesterId == request.providerId) {
                        throw IllegalStateException("Requester and provider cannot be the same user")
                    }

                    val learnerRef = firestore.collection("users").document(request.requesterId)
                    val teacherRef = firestore.collection("users").document(request.providerId)

                    val learnerSnap = transaction.get(learnerRef)
                    val teacherSnap = transaction.get(teacherRef)

                    val learnerProfile = learnerSnap.toObject(UserProfile::class.java)
                        ?: throw IllegalStateException("Learner profile not found")
                    val teacherProfile = teacherSnap.toObject(UserProfile::class.java)
                        ?: throw IllegalStateException("Teacher profile not found")

                    val duration = request.durationHours
                    if (learnerProfile.timeCreditBalance < duration) {
                        throw IllegalStateException("Learner has insufficient time credits (Balance: ${learnerProfile.timeCreditBalance}, Required: $duration)")
                    }

                    val newLearnerBalance = learnerProfile.timeCreditBalance - duration
                    val newTeacherBalance = teacherProfile.timeCreditBalance + duration

                    transaction.update(learnerRef, mapOf(
                        "timeCreditBalance" to newLearnerBalance,
                        "lastSwapId" to requestId
                    ))
                    transaction.update(teacherRef, mapOf(
                        "timeCreditBalance" to newTeacherBalance,
                        "lastSwapId" to requestId
                    ))
                    transaction.update(requestRef, "status", SwapRequestStatus.COMPLETED.name)

                    val spentTxDocId = "${requestId}_spent"
                    val spentTxRef = firestore.collection("transactions").document(spentTxDocId)
                    val spentTx = CreditTransaction(
                        id = spentTxDocId,
                        userId = request.requesterId,
                        sessionId = requestId,
                        type = TransactionType.SPENT,
                        amount = duration,
                        balanceAfter = newLearnerBalance
                    )
                    transaction.set(spentTxRef, spentTx)

                    val earnedTxDocId = "${requestId}_earned"
                    val earnedTxRef = firestore.collection("transactions").document(earnedTxDocId)
                    val earnedTx = CreditTransaction(
                        id = earnedTxDocId,
                        userId = request.providerId,
                        sessionId = requestId,
                        type = TransactionType.EARNED,
                        amount = duration,
                        balanceAfter = newTeacherBalance
                    )
                    transaction.set(earnedTxRef, earnedTx)

                    val otherUserId = if (callerId == request.requesterId) request.providerId else request.requesterId
                    val notifRef = firestore.collection("notifications").document()
                    val notifMessage = if (otherUserId == request.providerId) {
                        "Completed swap '${request.skillTitle}'. +$duration credits earned!"
                    } else {
                        "Completed swap '${request.skillTitle}'. -$duration credits."
                    }
                    transaction.set(notifRef, NotificationItem(
                        id = notifRef.id,
                        userId = otherUserId,
                        type = NotificationType.REQUEST_COMPLETED,
                        refId = requestId,
                        message = notifMessage
                    ))
                }.await()
            } else {
                firestore.collection("swapRequests").document(requestId)
                    .update("status", newStatus.name).await()

                val requestSnap = firestore.collection("swapRequests").document(requestId).get().await()
                val request = requestSnap.toObject(SwapRequest::class.java)
                if (request != null) {
                    val targetUserId = if (newStatus == SwapRequestStatus.ACCEPTED || newStatus == SwapRequestStatus.DECLINED) {
                        request.requesterId
                    } else {
                        request.providerId
                    }
                    val type = when (newStatus) {
                        SwapRequestStatus.ACCEPTED -> NotificationType.REQUEST_ACCEPTED
                        SwapRequestStatus.DECLINED -> NotificationType.REQUEST_DECLINED
                        SwapRequestStatus.CANCELLED -> NotificationType.REQUEST_CANCELLED
                        else -> NotificationType.REQUEST_CREATED
                    }
                    if (targetUserId != callerId) {
                        firestore.collection("notifications").add(
                            NotificationItem(
                                userId = targetUserId,
                                type = type,
                                refId = requestId,
                                message = "Your request for '${request.skillTitle}' was ${newStatus.name.lowercase()}."
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
