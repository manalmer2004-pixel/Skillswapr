package com.skillswap.app.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (!doc.exists()) return null

            val profile = doc.toObject(UserProfile::class.java)
            val isAdmin = checkIsAdmin(doc, profile)

            (profile ?: UserProfile(id = userId)).copy(isAdmin = isAdmin)
        } catch (e: Exception) {
            null
        }
    }

    private fun checkIsAdmin(doc: DocumentSnapshot, profile: UserProfile?): Boolean {
        if (!doc.exists()) return false
        if (profile?.isAdmin == true) return true

        val isAdminRaw = doc.get("isAdmin")
        if (isAdminRaw is Boolean && isAdminRaw) return true
        if (isAdminRaw is String && isAdminRaw.equals("true", ignoreCase = true)) return true

        return false
    }

    suspend fun updateUserProfile(profile: UserProfile): Boolean {
        return try {
            val editableFields = mapOf(
                "name" to profile.name,
                "handle" to profile.handle,
                "bio" to profile.bio,
                "location" to profile.location,
                "avatarUrl" to profile.avatarUrl,
                "skillsOffered" to profile.skillsOffered,
                "skillsWanted" to profile.skillsWanted
            )
            firestore.collection("users")
                .document(profile.id)
                .update(editableFields)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
