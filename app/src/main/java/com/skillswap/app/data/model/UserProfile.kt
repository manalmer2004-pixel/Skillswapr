package com.skillswap.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val handle: String = "@user",
    val email: String = "",
    val avatarUrl: String = "",
    val location: String = "Nairobi, Kenya",
    val distanceMiles: Double = 0.0,
    val bio: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val timeCreditBalance: Double = 0.0,
    val totalHoursEarned: Double = 0.0,
    val totalHoursSpent: Double = 0.0,
    val joinedDate: String = "",
    val skillsOffered: List<String> = emptyList(),
    val skillsWanted: List<String> = emptyList(),
    val badges: List<String> = emptyList(),
    @get:PropertyName("isVerified") val isVerified: Boolean = false,
    @get:PropertyName("isAdmin") val isAdmin: Boolean = false,
    val lastSwapId: String = ""
)
