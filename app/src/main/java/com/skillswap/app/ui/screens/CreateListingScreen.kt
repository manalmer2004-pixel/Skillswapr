package com.skillswap.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillswap.app.data.MockData
import com.skillswap.app.data.model.SkillListing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onDismiss: () -> Unit,
    onPublish: (SkillListing) -> Unit
) {
    var listingType by remember { mutableStateOf("offer") } // "offer" or "request"
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MockData.SKILL_CATEGORIES[1]) }
    var description by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableStateOf(2.0f) }
    var toolsProvidedInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post a Skill Swap") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            Row(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(selectedTabIndex = MockData.SKILL_CATEGORIES.indexOf(selectedCategory).coerceAtLeast(0)) {
                    MockData.SKILL_CATEGORIES.drop(1).forEach { cat ->
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            text = { Text(cat) }
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
                    value = estimatedHours,
                    onValueChange = { estimatedHours = it },
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
                    if (title.isNotBlank()) {
                        val newListing = SkillListing(
                            id = "listing_${System.currentTimeMillis()}",
                            userId = "user_maya",
                            type = listingType,
                            title = title,
                            category = selectedCategory,
                            description = description,
                            estimatedHours = estimatedHours.toDouble(),
                            creditCost = estimatedHours.toDouble(),
                            toolsProvided = toolsProvidedInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            teacherName = "Maya Lin",
                            teacherRating = 4.9
                        )
                        onPublish(newListing)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Publish Listing", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
