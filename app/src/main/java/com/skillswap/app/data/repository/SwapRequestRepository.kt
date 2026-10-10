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
    suspend fun createSwapRequest(request: SwapRequest): Boolean {
        return try {
            val docRef = firestore.collection("swapRequests").document()
            val requestWithId = request.copy(id = docRef.id)
            docRef.set(requestWithId).await()

            val notification = NotificationItem(
                userId = request.providerId,
                type = NotificationType.REQUEST_CREATED,
                refId = docRef.id,
                message = "New swap request for '${request.skillTitle}'"
            )
            firestore.collection("notifications").add(notification).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSwapRequestsFlow(userId: String): Flow<List<SwapRequest>> = callbackFlow {
        val listener = firestore.collection("swapRequests")
            .whereIn("requesterId", listOf(userId))
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
            .whereIn("providerId", listOf(userId))
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

    suspend fun updateStatus(requestId: String, newStatus: SwapRequestStatus): Result<Unit> {
        return try {
            if (newStatus == SwapRequestStatus.COMPLETED) {
                firestore.runTransaction { transaction ->
                    val requestRef = firestore.collection("swapRequests").document(requestId)
                    val requestSnap = transaction.get(requestRef)
                    val request = requestSnap.toObject(SwapRequest::class.java)
                        ?: throw IllegalStateException("Swap request not found")

                    if (request.status == SwapRequestStatus.COMPLETED) {
                        return@runTransaction
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

                    transaction.update(learnerRef, "timeCreditBalance", newLearnerBalance)
                    transaction.update(teacherRef, "timeCreditBalance", newTeacherBalance)
                    transaction.update(requestRef, "status", SwapRequestStatus.COMPLETED.name)

                    val spentTxRef = firestore.collection("transactions").document()
                    val spentTx = CreditTransaction(
                        id = spentTxRef.id,
                        userId = request.requesterId,
                        sessionId = requestId,
                        type = TransactionType.SPENT,
                        amount = duration,
                        balanceAfter = newLearnerBalance
                    )
                    transaction.set(spentTxRef, spentTx)

                    val earnedTxRef = firestore.collection("transactions").document()
                    val earnedTx = CreditTransaction(
                        id = earnedTxRef.id,
                        userId = request.providerId,
                        sessionId = requestId,
                        type = TransactionType.EARNED,
                        amount = duration,
                        balanceAfter = newTeacherBalance
                    )
                    transaction.set(earnedTxRef, earnedTx)

                    val notif1Ref = firestore.collection("notifications").document()
                    transaction.set(notif1Ref, NotificationItem(
                        id = notif1Ref.id,
                        userId = request.requesterId,
                        type = NotificationType.REQUEST_COMPLETED,
                        refId = requestId,
                        message = "Completed swap '${request.skillTitle}'. -$duration credits."
                    ))

                    val notif2Ref = firestore.collection("notifications").document()
                    transaction.set(notif2Ref, NotificationItem(
                        id = notif2Ref.id,
                        userId = request.providerId,
                        type = NotificationType.REQUEST_COMPLETED,
                        refId = requestId,
                        message = "Completed swap '${request.skillTitle}'. +$duration credits earned!"
                    ))
                }.await()
            } else {
                firestore.collection("swapRequests").document(requestId)
                    .update("status", newStatus.name).await()

                val requestSnap = firestore.collection("swapRequests").document(requestId).get().await()
                val request = requestSnap.toObject(SwapRequest::class.java)
                if (request != null) {
                    val targetUserId = if (newStatus == SwapRequestStatus.ACCEPTED || newStatus == SwapRequestStatus.DECLINED) request.requesterId else request.providerId
                    val type = when (newStatus) {
                        SwapRequestStatus.ACCEPTED -> NotificationType.REQUEST_ACCEPTED
                        SwapRequestStatus.DECLINED -> NotificationType.REQUEST_DECLINED
                        else -> NotificationType.REQUEST_CREATED
                    }
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
