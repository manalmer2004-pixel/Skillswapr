package com.skillswap.app.util

import android.location.Location
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import java.util.Locale
import kotlin.math.roundToInt

object LocationUtils {

    /**
     * Rounds a coordinate to 3 decimal places (~100 meters) to protect user privacy.
     */
    fun roundCoordinate(value: Double): Double {
        return (value * 1000.0).roundToInt() / 1000.0
    }

    /**
     * Computes the distance in kilometers between two lat/lng points using Android's Location.distanceTo.
     */
    fun computeDistanceKm(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return (results[0] / 1000.0)
    }

    /**
     * Formats distance in km or meters nicely.
     */
    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            val meters = (distanceKm * 1000).roundToInt()
            "$meters m away"
        } else {
            String.format(Locale.US, "%.1f km away", distanceKm)
        }
    }

    /**
     * Generates a GeoHash string for a given latitude and longitude.
     */
    fun computeGeoHash(lat: Double, lng: Double): String {
        return GeoFireUtils.getGeoHashForLocation(GeoLocation(lat, lng))
    }
}
