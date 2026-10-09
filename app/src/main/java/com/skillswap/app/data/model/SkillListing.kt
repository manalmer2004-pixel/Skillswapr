package com.skillswap.app.data.model

import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillListing(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val type: String = "offer", // "offer" or "request"
    val title: String = "",
    val category: String = "Water & Sanitation",
    val description: String = "",
    val imageUrl: String = "",
    @DrawableRes val imageResId: Int? = null,
    val estimatedHours: Double = 1.0,
    val creditCost: Double = 1.0,
    val locationArea: String = "Nairobi, Kenya",
    val distanceMiles: Double = 0.5,
    val availability: String = "Weekends",
    val toolsProvided: List<String> = emptyList(),
    val createdAt: String = "Today",
    val teacherName: String = "",
    val teacherAvatar: String = "",
    val teacherRating: Double = 5.0,
    val saved: Boolean = false
)
