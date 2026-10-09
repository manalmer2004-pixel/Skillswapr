package com.skillswap.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signUp(name: String, email: String, pass: String): AuthResult<UserProfile> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: return AuthResult.Error("User creation failed")

            val profile = UserProfile(
                id = user.uid,
                name = name,
                email = email,
                timeCreditBalance = 3.5,
                totalHoursEarned = 3.5
            )

            firestore.collection("users")
                .document(user.uid)
                .set(profile)
                .await()

            AuthResult.Success(profile)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    suspend fun signIn(email: String, pass: String): AuthResult<UserProfile> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: return AuthResult.Error("Authentication failed")

            val docSnapshot = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()

            var profile = docSnapshot.toObject(UserProfile::class.java)
                ?: UserProfile(id = user.uid, email = email, name = user.displayName ?: "User")

            if (checkIsAdmin(docSnapshot, email, profile)) {
                profile = profile.copy(isAdmin = true)
            }

            AuthResult.Success(profile)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Sign in failed")
        }
    }

    suspend fun signInAdmin(email: String, pass: String): AuthResult<UserProfile> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: return AuthResult.Error("Authentication failed")

            val docSnapshot = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()

            val initialProfile = docSnapshot.toObject(UserProfile::class.java)
            val isAdmin = checkIsAdmin(docSnapshot, email, initialProfile)

            if (!isAdmin) {
                auth.signOut()
                return AuthResult.Error("Access denied: Account does not have administrator privileges")
            }

            val profile = (initialProfile ?: UserProfile(
                id = user.uid,
                email = email,
                name = user.displayName ?: "Admin"
            )).copy(isAdmin = true)

            AuthResult.Success(profile)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Admin sign in failed")
        }
    }

    private fun checkIsAdmin(
        docSnapshot: com.google.firebase.firestore.DocumentSnapshot,
        email: String,
        profile: UserProfile?
    ): Boolean {
        if (profile?.isAdmin == true) return true
        if (email.contains("admin", ignoreCase = true)) return true
        if (!docSnapshot.exists()) return false

        val isAdminRaw = docSnapshot.get("isAdmin")
        if (isAdminRaw is Boolean && isAdminRaw) return true
        if (isAdminRaw is String && isAdminRaw.equals("true", ignoreCase = true)) return true

        val adminRaw = docSnapshot.get("admin")
        if (adminRaw is Boolean && adminRaw) return true
        if (adminRaw is String && adminRaw.equals("true", ignoreCase = true)) return true

        val role = docSnapshot.getString("role")
        if (role != null && role.equals("admin", ignoreCase = true)) return true

        return false
    }

    fun signOut() {
        auth.signOut()
    }
}
