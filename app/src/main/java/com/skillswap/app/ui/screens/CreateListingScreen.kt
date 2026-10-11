package com.skillswap.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.data.repository.StorageRepository
import com.skillswap.app.ui.viewmodel.AuthViewModel
import com.skillswap.app.ui.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    currentUserId: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onPublish: (SkillListing) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val userProfile = authUiState.userProfile

    val categoriesState by categoryViewModel.categories.collectAsState()
    val categories = remember(categoriesState) {
        if (categoriesState.isEmpty()) listOf("Water & Sanitation", "Cooking", "Gardening", "Other")
        else categoriesState.map { it.name }
    }

    var listingType by remember { mutableStateOf("offer") } // "offer" or "request"
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    val selectedCategoryIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
    var description by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableDoubleStateOf(2.0) }
    var toolsProvidedInput by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val storageRepository = remember { StorageRepository() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post a Skill Swap") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
            if (currentUserId.isBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "You must be signed in to publish a skill swap.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Type Selector (Offer vs Request)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = listingType == "offer",
                    onClick = { listingType = "offer" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Offer a Skill")
                }
                SegmentedButton(
                    selected = listingType == "request",
                    onClick = { listingType = "request" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Request a Skill")
                }
            }

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. Water Filter Maintenance, Basic First Aid") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Selection
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat) }
                    )
                }
            }

            // Image Picker Section
            Text("Skill Image (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Skill Image Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Image")
                    }
                }
            } else {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Image",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap to add a photo of your skill or equipment",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Describe what you will teach or what help you need...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Hours / Credits Slider
            Column {
                Text(
                    text = "Estimated Time: ${estimatedHours.toInt()} Hours = ${estimatedHours.toInt()} Time Credits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = estimatedHours.toFloat(),
                    onValueChange = { estimatedHours = it.toDouble() },
                    valueRange = 1.0f..5.0f,
                    steps = 3
                )
            }

            // Tools Input
            OutlinedTextField(
                value = toolsProvidedInput,
                onValueChange = { toolsProvidedInput = it },
                label = { Text("Tools / Materials Provided (comma separated)") },
                placeholder = { Text("e.g. Filter Media, Brushes, Notebook") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Publish CTA Button
            Button(
                onClick = {
                    if (title.isNotBlank() && currentUserId.isNotBlank()) {
                        isPublishing = true
                        scope.launch {
                            val listingId = "listing_${System.currentTimeMillis()}"
                            var imageUrl = ""
                            selectedImageUri?.let { uri ->
                                imageUrl = storageRepository.uploadSkillImage(uri, listingId) ?: uri.toString()
                            }

                            val newListing = SkillListing(
                                id = listingId,
                                userId = currentUserId,
                                type = listingType,
                                title = title,
                                category = selectedCategory,
                                description = description,
                                imageUrl = imageUrl,
                                estimatedHours = estimatedHours,
                                creditCost = estimatedHours,
                                toolsProvided = toolsProvidedInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                teacherName = currentUserName,
                                teacherRating = userProfile?.rating ?: 0.0,
                                reviewCount = userProfile?.reviewCount ?: 0,
                                locationArea = userProfile?.location ?: "",
                                createdAt = System.currentTimeMillis()
                            )
                            isPublishing = false
                            onPublish(newListing)
                        }
                    }
                },
                enabled = title.isNotBlank() && currentUserId.isNotBlank() && !isPublishing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Publish Listing", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
