package com.skillswap.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.skillswap.app.BuildConfig
import com.skillswap.app.R
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.ui.viewmodel.AuthViewModel
import com.skillswap.app.ui.viewmodel.CategoryViewModel
import com.skillswap.app.ui.viewmodel.MapViewModel
import com.skillswap.app.util.LocationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onSelectSkill: (SkillListing) -> Unit,
    onNavigateToList: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by mapViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val categoriesState by categoryViewModel.categories.collectAsState()

    val currentUserId = authState.userProfile?.id ?: ""
    val currentUserName = authState.userProfile?.name ?: ""

    val categoryNames = remember(categoriesState) {
        listOf("All") + categoriesState.map { it.name }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(uiState.mapCenterLatitude, uiState.mapCenterLongitude),
            12f
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        mapViewModel.checkPermissionAndFetchLocation()
    }

    // Automatically request location permission on open if not granted
    LaunchedEffect(Unit) {
        if (!uiState.hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Re-check permissions and location on ON_RESUME
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mapViewModel.checkPermissionAndFetchLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Move camera when user position changes
    LaunchedEffect(uiState.userLatitude, uiState.userLongitude) {
        if (uiState.userLatitude != null && uiState.userLongitude != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(uiState.userLatitude!!, uiState.userLongitude!!),
                    13f
                )
            )
        }
    }

    // Detect user gesture panning (ignore programmatic camera moves)
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            val target = cameraPositionState.position.target
            if (LocationUtils.computeDistanceKm(target.latitude, target.longitude, uiState.mapCenterLatitude, uiState.mapCenterLongitude) > 0.3) {
                mapViewModel.onMapPanned(target.latitude, target.longitude)
            }
        }
    }

    val mapProperties = remember(uiState.hasLocationPermission) {
        MapProperties(isMyLocationEnabled = uiState.hasLocationPermission)
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.map_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.nearbySkills.size} skills within ${uiState.selectedRadiusKm.toInt()} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToList) {
                        Icon(Icons.Default.List, contentDescription = "Switch to List View")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Google Map Display
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings
            ) {
                // Skill Markers using rememberMarkerState to avoid state recreation on recomposition
                uiState.nearbySkills.forEach { (skill, distKm) ->
                    val lat = skill.latitude
                    val lng = skill.longitude
                    if (lat != null && lng != null) {
                        val markerState = rememberMarkerState(key = skill.id, position = LatLng(lat, lng))
                        Marker(
                            state = markerState,
                            title = skill.title,
                            snippet = "${skill.category} · ${LocationUtils.formatDistance(distKm)}",
                            onClick = {
                                mapViewModel.selectSkill(skill, distKm)
                                true
                            }
                        )
                    }
                }
            }

            // Top Filter Chips Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(categoryNames) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { mapViewModel.updateCategory(category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    }
                }

                // Radius Chips (1 km, 5 km, 10 km, 25 km)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.0, 5.0, 10.0, 25.0).forEach { radius ->
                        FilterChip(
                            selected = uiState.selectedRadiusKm == radius,
                            onClick = { mapViewModel.updateRadius(radius) },
                            label = { Text("${radius.toInt()} km") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    }
                }
            }

            // "Search this area" Button when user panned map
            if (uiState.isMapPanned) {
                Button(
                    onClick = {
                        val target = cameraPositionState.position.target
                        mapViewModel.searchNearbySkills(target.latitude, target.longitude)
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 110.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.search_this_area),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Location Permission Rationale Banner at bottom (stacked above FABs/Cards)
            if (!uiState.hasLocationPermission) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 110.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.location_permission_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.location_permission_rationale),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text(stringResource(R.string.open_settings))
                        }
                    }
                }
            }

            // Floating Action Buttons (My Location & Seed Debug Skills)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Debug Seed Button for Nairobi Demo (Gated by BuildConfig.DEBUG)
                if (BuildConfig.DEBUG) {
                    SmallFloatingActionButton(
                        onClick = {
                            mapViewModel.seedDebugNairobiSkills(currentUserId, currentUserName)
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Seed Demo Skills")
                    }
                }

                // Recenter My Location FAB (Animates camera directly to user position)
                FloatingActionButton(
                    onClick = {
                        if (uiState.hasLocationPermission) {
                            val lat = uiState.userLatitude
                            val lng = uiState.userLongitude
                            if (lat != null && lng != null) {
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 14f)
                                    )
                                }
                            } else {
                                mapViewModel.checkPermissionAndFetchLocation()
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }

            // Empty state if no skills found nearby
            if (!uiState.isLoading && uiState.nearbySkills.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_skills_nearby),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { mapViewModel.updateRadius(25.0) }) {
                            Text("Expand Radius to 25 km")
                        }
                    }
                }
            }

            // Bottom Card for Selected Skill
            uiState.selectedSkill?.let { skill ->
                val distanceKm = uiState.selectedSkillDistanceKm
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = skill.category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            TextButton(onClick = { mapViewModel.clearSelectedSkill() }) {
                                Text("Close")
                            }
                        }

                        Text(
                            text = skill.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Teacher: ${skill.teacherName.ifBlank { "Neighbor" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (distanceKm != null) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = LocationUtils.formatDistance(distanceKm),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val label = skill.locationLabel.ifBlank { skill.locationArea }
                            if (label.isNotBlank()) {
                                Text("·", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.privacy_notice),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val lat = skill.latitude
                                    val lng = skill.longitude
                                    if (lat != null && lng != null) {
                                        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(skill.title)})")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.open_in_google_maps))
                            }

                            Button(
                                onClick = {
                                    mapViewModel.clearSelectedSkill()
                                    onSelectSkill(skill)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.view_skill))
                            }
                        }
                    }
                }
            }
        }
    }
}
