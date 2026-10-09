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
        if (profile?.isAdmin == true) return true

        val isAdminRaw = doc.get("isAdmin")
        if (isAdminRaw is Boolean && isAdminRaw) return true
        if (isAdminRaw is String && isAdminRaw.equals("true", ignoreCase = true)) return true

        val adminRaw = doc.get("admin")
        if (adminRaw is Boolean && adminRaw) return true
        if (adminRaw is String && adminRaw.equals("true", ignoreCase = true)) return true

        val role = doc.getString("role")
        if (role != null && role.equals("admin", ignoreCase = true)) return true

        val email = doc.getString("email") ?: ""
        if (email.contains("admin", ignoreCase = true)) return true

        return false
    }

    suspend fun updateUserProfile(profile: UserProfile): Boolean {
        return try {
            firestore.collection("users")
                .document(profile.id)
                .set(profile)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
