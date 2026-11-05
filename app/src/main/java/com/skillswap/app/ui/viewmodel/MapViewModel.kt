package com.skillswap.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.DeliveryMode
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.data.repository.LocationRepository
import com.skillswap.app.data.repository.SkillRepository
import com.skillswap.app.util.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val mapCenterLatitude: Double = -1.286389, // Nairobi CBD default center
    val mapCenterLongitude: Double = 36.817223,
    val selectedRadiusKm: Double = 5.0,
    val selectedCategory: String = "All",
    val selectedDeliveryMode: String = "All", // "All", "IN_PERSON", "ONLINE", "BOTH"
    val nearbySkills: List<Pair<SkillListing, Double>> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedSkill: SkillListing? = null,
    val selectedSkillDistanceKm: Double? = null,
    val hasLocationPermission: Boolean = false,
    val isMapPanned: Boolean = false
)

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val skillRepository = SkillRepository()
    private val locationRepository = LocationRepository(application)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndFetchLocation()
    }

    fun checkPermissionAndFetchLocation() {
        val hasPermission = locationRepository.hasLocationPermission()
        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission)

        if (hasPermission) {
            viewModelScope.launch {
                val loc = locationRepository.getCurrentLocation()
                if (loc != null) {
                    val lat = loc.latitude
                    val lng = loc.longitude
                    _uiState.value = _uiState.value.copy(
                        userLatitude = lat,
                        userLongitude = lng,
                        mapCenterLatitude = lat,
                        mapCenterLongitude = lng
                    )
                    searchNearbySkills(lat, lng)
                } else {
                    // Default to Nairobi center if current location fails
                    searchNearbySkills(_uiState.value.mapCenterLatitude, _uiState.value.mapCenterLongitude)
                }
            }
        } else {
            // Load skills around default Nairobi center when no location permission
            searchNearbySkills(_uiState.value.mapCenterLatitude, _uiState.value.mapCenterLongitude)
        }
    }

    fun searchNearbySkills(
        centerLat: Double = _uiState.value.mapCenterLatitude,
        centerLng: Double = _uiState.value.mapCenterLongitude
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isMapPanned = false)
            try {
                val results = skillRepository.getNearbySkills(
                    centerLat = centerLat,
                    centerLng = centerLng,
                    radiusKm = _uiState.value.selectedRadiusKm,
                    categoryFilter = _uiState.value.selectedCategory,
                    deliveryModeFilter = _uiState.value.selectedDeliveryMode
                )
                _uiState.value = _uiState.value.copy(
                    nearbySkills = results,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Error loading nearby skills"
                )
            }
        }
    }

    fun onMapPanned(centerLat: Double, centerLng: Double) {
        _uiState.value = _uiState.value.copy(
            mapCenterLatitude = centerLat,
            mapCenterLongitude = centerLng,
            isMapPanned = true
        )
    }

    fun updateRadius(radiusKm: Double) {
        _uiState.value = _uiState.value.copy(selectedRadiusKm = radiusKm)
        searchNearbySkills()
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        searchNearbySkills()
    }

    fun updateDeliveryMode(mode: String) {
        _uiState.value = _uiState.value.copy(selectedDeliveryMode = mode)
        searchNearbySkills()
    }

    fun selectSkill(skill: SkillListing?, distanceKm: Double? = null) {
        _uiState.value = _uiState.value.copy(
            selectedSkill = skill,
            selectedSkillDistanceKm = distanceKm
        )
    }

    fun clearSelectedSkill() {
        _uiState.value = _uiState.value.copy(selectedSkill = null, selectedSkillDistanceKm = null)
    }

    /**
     * Seeds sample skills with Nairobi area coordinates (Westlands, Kilimani, CBD, Karen, USIU) for demoing.
     */
    fun seedDebugNairobiSkills(currentUserId: String, currentUserName: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val sampleNairobiSkills = listOf(
                SkillListing(
                    id = "demo_nairobi_1",
                    userId = if (currentUserId.isNotBlank()) currentUserId else "teacher_nairobi_1",
                    teacherName = if (currentUserName.isNotBlank()) currentUserName else "Kamau Njenga",
                    type = "offer",
                    title = "Urban Farming & Soil Health",
                    category = "Gardening",
                    description = "Learn organic composting, drip irrigation, and crop rotation for small urban plots.",
                    estimatedHours = 2.0,
                    creditCost = 2.0,
                    locationArea = "Westlands, Nairobi",
                    locationLabel = "Westlands Public Library",
                    latitude = -1.2672,
                    longitude = 36.8116,
                    geohash = LocationUtils.computeGeoHash(-1.267, 36.812),
                    deliveryMode = DeliveryMode.IN_PERSON.name
                ),
                SkillListing(
                    id = "demo_nairobi_2",
                    userId = "teacher_nairobi_2",
                    teacherName = "Amina Hassan",
                    type = "offer",
                    title = "Solar Panel Maintenance & Basic Wiring",
                    category = "Other",
                    description = "Hands-on solar installation troubleshooting, battery setup, and inverter maintenance.",
                    estimatedHours = 3.0,
                    creditCost = 3.0,
                    locationArea = "Kilimani, Nairobi",
                    locationLabel = "Yaya Center Cafe",
                    latitude = -1.2891,
                    longitude = 36.7886,
                    geohash = LocationUtils.computeGeoHash(-1.289, 36.789),
                    deliveryMode = DeliveryMode.IN_PERSON.name
                ),
                SkillListing(
                    id = "demo_nairobi_3",
                    userId = "teacher_nairobi_3",
                    teacherName = "David Ochieng",
                    type = "offer",
                    title = "Kotlin & Android App Architecture",
                    category = "Coding",
                    description = "Master Jetpack Compose, Coroutines, and Clean Architecture for Android development.",
                    estimatedHours = 1.0,
                    creditCost = 1.0,
                    locationArea = "USIU Area, Kasarani",
                    locationLabel = "USIU Library Campus",
                    latitude = -1.2173,
                    longitude = 36.8872,
                    geohash = LocationUtils.computeGeoHash(-1.217, 36.887),
                    deliveryMode = DeliveryMode.BOTH.name
                ),
                SkillListing(
                    id = "demo_nairobi_4",
                    userId = "teacher_nairobi_4",
                    teacherName = "Grace Wanjiku",
                    type = "offer",
                    title = "First Aid & Community Health",
                    category = "Fitness",
                    description = "CPR certification prep, wound dressing, and emergency response training.",
                    estimatedHours = 2.0,
                    creditCost = 2.0,
                    locationArea = "CBD, Nairobi",
                    locationLabel = "Nairobi City Hall Plaza",
                    latitude = -1.286389,
                    longitude = 36.817223,
                    geohash = LocationUtils.computeGeoHash(-1.286, 36.817),
                    deliveryMode = DeliveryMode.IN_PERSON.name
                ),
                SkillListing(
                    id = "demo_nairobi_5",
                    userId = "teacher_nairobi_5",
                    teacherName = "John Mutua",
                    type = "offer",
                    title = "Clean Water Storage & Biofiltration",
                    category = "Water & Sanitation",
                    description = "How to build and maintain low-cost sand filters and safe rainwater harvesting tanks.",
                    estimatedHours = 2.0,
                    creditCost = 2.0,
                    locationArea = "Karen, Nairobi",
                    locationLabel = "Karen Shopping Center",
                    latitude = -1.3197,
                    longitude = 36.7062,
                    geohash = LocationUtils.computeGeoHash(-1.320, 36.706),
                    deliveryMode = DeliveryMode.IN_PERSON.name
                )
            )

            for (skill in sampleNairobiSkills) {
                skillRepository.addSkill(skill)
            }

            searchNearbySkills()
            onDone()
        }
    }
}
