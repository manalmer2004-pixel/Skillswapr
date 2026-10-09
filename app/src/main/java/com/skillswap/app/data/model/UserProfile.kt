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
    val rating: Double = 5.0,
    val reviewCount: Int = 0,
    val timeCreditBalance: Double = 3.5,
    val totalHoursEarned: Double = 8.0,
    val totalHoursSpent: Double = 4.5,
    val joinedDate: String = "Apr 2024",
    val skillsOffered: List<String> = emptyList(),
    val skillsWanted: List<String> = emptyList(),
    val badges: List<String> = emptyList(),
    @get:PropertyName("isVerified") val isVerified: Boolean = true,
    @get:PropertyName("isAdmin") val isAdmin: Boolean = false
)
