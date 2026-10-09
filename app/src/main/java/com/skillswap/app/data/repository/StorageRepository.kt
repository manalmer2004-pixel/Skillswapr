package com.skillswap.app.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun uploadSkillImage(uri: Uri, skillId: String): String? {
        return try {
            val ref = storage.reference.child("skills/$skillId.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            // Fallback to local URI string
            uri.toString()
        }
    }
}
