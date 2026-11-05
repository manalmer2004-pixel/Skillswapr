package com.skillswap.app.data.repository

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.DeliveryMode
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.util.LocationUtils
import kotlinx.coroutines.tasks.await

class SkillRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getSkills(): List<SkillListing> {
        return try {
            val snapshot = firestore.collection("skills").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SkillListing::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSkill(skill: SkillListing): Boolean {
        return try {
            val roundedSkill = if (skill.latitude != null && skill.longitude != null) {
                val roundedLat = LocationUtils.roundCoordinate(skill.latitude)
                val roundedLng = LocationUtils.roundCoordinate(skill.longitude)
                val computedHash = LocationUtils.computeGeoHash(roundedLat, roundedLng)
                skill.copy(
                    latitude = roundedLat,
                    longitude = roundedLng,
                    geohash = computedHash
                )
            } else {
                skill
            }

            firestore.collection("skills")
                .document(roundedSkill.id)
                .set(roundedSkill)
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

    suspend fun getNearbySkills(
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double,
        categoryFilter: String = "All",
        deliveryModeFilter: String = "All"
    ): List<Pair<SkillListing, Double>> {
        val center = GeoLocation(centerLat, centerLng)
        val radiusInMeters = radiusKm * 1000.0
        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeters)

        val fetchedDocs = mutableListOf<SkillListing>()

        for (b in bounds) {
            try {
                val snapshot = firestore.collection("skills")
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash)
                    .get()
                    .await()

                val docs = snapshot.documents.mapNotNull { doc ->
                    try { doc.toObject(SkillListing::class.java) } catch (_: Exception) { null }
                }
                fetchedDocs.addAll(docs)
            } catch (_: Exception) {
                // If geohash query fails (e.g. missing index or offline), fallback to fetching all skills
                val allDocs = getSkills()
                fetchedDocs.addAll(allDocs)
                break
            }
        }

        val resultList = mutableListOf<Pair<SkillListing, Double>>()
        val seenIds = mutableSetOf<String>()

        for (skill in fetchedDocs) {
            if (skill.id.isBlank() || !seenIds.add(skill.id)) continue

            // Filter out delivery mode mismatch
            if (deliveryModeFilter == "IN_PERSON" && skill.deliveryMode == DeliveryMode.ONLINE.name) continue
            if (deliveryModeFilter == "ONLINE" && skill.deliveryMode == DeliveryMode.IN_PERSON.name) continue

            // Filter category mismatch
            if (categoryFilter != "All" && !skill.category.equals(categoryFilter, ignoreCase = true)) continue

            val lat = skill.latitude
            val lng = skill.longitude

            if (lat != null && lng != null) {
                val distKm = LocationUtils.computeDistanceKm(centerLat, centerLng, lat, lng)
                if (distKm <= radiusKm) {
                    resultList.add(Pair(skill, distKm))
                }
            }
        }

        return resultList.sortedBy { it.second }
    }
}
