package com.skillswap.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.SkillListing
import kotlinx.coroutines.tasks.await

class SkillRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getSkills(): List<SkillListing> {
        return try {
            val snapshot = firestore.collection("skills").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(SkillListing::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSkill(skill: SkillListing): Boolean {
        return try {
            firestore.collection("skills")
                .document(skill.id)
                .set(skill)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteSkill(skillId: String): Boolean {
        return try {
            firestore.collection("skills")
                .document(skillId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
