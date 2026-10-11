package com.skillswap.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.data.model.SwapRequest
import com.skillswap.app.data.model.UserAvailability
import com.skillswap.app.ui.viewmodel.AvailabilityViewModel
import com.skillswap.app.ui.viewmodel.SwapRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listing: SkillListing,
    currentUserId: String,
    onBack: () -> Unit,
    onNavigateToRequests: () -> Unit = {},
    onStartChat: (String) -> Unit = {},
    requestViewModel: SwapRequestViewModel = viewModel(),
    availabilityViewModel: AvailabilityViewModel = viewModel()
) {
    val isOwner = currentUserId.isNotBlank() && currentUserId == listing.userId
    val snackbarHostState = remember { SnackbarHostState() }

    var showRequestDialog by remember { mutableStateOf(false) }
    var proposedTimeText by remember { mutableStateOf("Tomorrow at 10:00 AM") }
    var durationHoursInput by remember { mutableDoubleStateOf(listing.estimatedHours) }

    var showAddAvailabilityDialog by remember { mutableStateOf(false) }
    var newDayOfWeek by remember { mutableStateOf("Monday") }
    var newStartTime by remember { mutableStateOf("09:00") }
    var newEndTime by remember { mutableStateOf("17:00") }

    val teacherAvailabilities by availabilityViewModel.availabilities.collectAsState()

    LaunchedEffect(listing.userId) {
        if (listing.userId.isNotBlank()) {
            availabilityViewModel.loadAvailability(listing.userId)
        }
    }

    // Request Swap Dialog
    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = { Text("Request Skill Swap") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Skill: ${listing.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = proposedTimeText,
                        onValueChange = { proposedTimeText = it },
                        label = { Text("Proposed Date & Time") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = durationHoursInput.toString(),
                        onValueChange = { durationHoursInput = it.toDoubleOrNull() ?: 1.0 },
                        label = { Text("Duration (Hours)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRequestDialog = false
                        val request = SwapRequest(
                            requesterId = currentUserId,
                            providerId = listing.userId,
                            skillId = listing.id,
                            skillTitle = listing.title,
                            providerName = listing.teacherName,
                            proposedTime = proposedTimeText,
                            durationHours = durationHoursInput
                        )
                        requestViewModel.createRequest(request) {
                            onNavigateToRequests()
                        }
                    }
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Availability Slot Dialog (Owner only)
    if (showAddAvailabilityDialog) {
        AlertDialog(
            onDismissRequest = { showAddAvailabilityDialog = false },
            title = { Text("Add Availability Slot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newDayOfWeek,
                        onValueChange = { newDayOfWeek = it },
                        label = { Text("Day of Week") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newStartTime,
                        onValueChange = { newStartTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEndTime,
                        onValueChange = { newEndTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        availabilityViewModel.saveAvailability(
                            UserAvailability(
                                userId = currentUserId,
                                dayOfWeek = newDayOfWeek,
                                startTime = newStartTime,
                                endTime = newEndTime
                            )
                        )
                        showAddAvailabilityDialog = false
                    }
                ) {
                    Text("Add Slot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAvailabilityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(listing.category) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rate / Cost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${listing.creditCost} Time Credits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isOwner) {
                            OutlinedButton(
                                onClick = { onStartChat(listing.userId) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat")
                            }
                        }

                        Button(
                            onClick = { showRequestDialog = true },
                            enabled = !isOwner,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isOwner) "Your Listing" else "Request Swap")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Image if available
            if (!listing.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (listing.imageResId != null) {
                Image(
                    painter = painterResource(id = listing.imageResId),
                    contentDescription = listing.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Category & Type Chip
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (listing.type == "offer") "OFFERING SKILL" else "REQUESTING SKILL",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Title
            Text(
                text = listing.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Provider Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = listing.teacherName.ifBlank { "U" }.take(1),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = listing.teacherName.ifBlank { "Neighbor" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (listing.reviewCount > 0 && listing.teacherRating > 0.0) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${listing.teacherRating} (${listing.reviewCount})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "New Member",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            if (listing.locationArea.isNotBlank() || listing.distanceMiles > 0.0) {
                                Text("·", style = MaterialTheme.typography.labelSmall)
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                val locationText = buildString {
                                    if (listing.locationArea.isNotBlank()) append(listing.locationArea)
                                    if (listing.distanceMiles > 0.0) {
                                        if (isNotEmpty()) append(" · ")
                                        append("${listing.distanceMiles} miles away")
                                    }
                                }
                                Text(
                                    text = locationText,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Availability Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Teacher Availability",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isOwner) {
                            IconButton(
                                onClick = { showAddAvailabilityDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Slot")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (teacherAvailabilities.isEmpty()) {
                        Text(
                            text = "No specific availability slots listed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            teacherAvailabilities.forEach { slot ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${slot.dayOfWeek}: ${slot.startTime} - ${slot.endTime}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (isOwner) {
                                        IconButton(
                                            onClick = { availabilityViewModel.deleteAvailability(slot.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Description
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Time & Availability
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Text(
                            text = "Estimated Time: ${listing.estimatedHours} hrs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Tools Provided
            if (listing.toolsProvided.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tools / Materials Provided",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    listing.toolsProvided.forEach { tool ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(text = tool, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
