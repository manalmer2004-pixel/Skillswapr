package com.skillswap.app.data.model

import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint

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
    val locationArea: String = "",
    val distanceMiles: Double = 0.0,
    val availability: String = "",
    val toolsProvided: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val teacherName: String = "",
    val teacherAvatar: String = "",
    val saved: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geohash: String = "",
    val locationLabel: String = "",
    val deliveryMode: String = DeliveryMode.IN_PERSON.name
) {
    @get:Exclude
    @get:Ignore
    val location: GeoPoint?
        get() = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null

    @get:Exclude
    @get:Ignore
    val deliveryModeEnum: DeliveryMode
        get() = try {
            DeliveryMode.valueOf(deliveryMode)
        } catch (_: Exception) {
            DeliveryMode.IN_PERSON
        }
}
