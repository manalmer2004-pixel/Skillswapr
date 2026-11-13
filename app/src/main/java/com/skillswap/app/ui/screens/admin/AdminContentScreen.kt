package com.skillswap.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.app.data.model.Category
import com.skillswap.app.data.model.Review
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.ui.viewmodel.AdminContentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentScreen(
    adminContentViewModel: AdminContentViewModel = viewModel()
) {
    val uiState by adminContentViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryNameInput by remember { mutableStateOf("") }
    var categoryOrderInput by remember { mutableStateOf("1") }

    var selectedCategoryForEdit by remember { mutableStateOf<Category?>(null) }
    var editCategoryNameInput by remember { mutableStateOf("") }
    var editCategoryOrderInput by remember { mutableStateOf("1") }

    var skillToDelete by remember { mutableStateOf<SkillListing?>(null) }
    var reviewToDelete by remember { mutableStateOf<Review?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminContentViewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminContentViewModel.clearMessages()
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { categoryNameInput = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = categoryOrderInput,
                        onValueChange = { categoryOrderInput = it },
                        label = { Text("Order Index") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val order = categoryOrderInput.toIntOrNull() ?: 1
                        if (categoryNameInput.isNotBlank()) {
                            showAddCategoryDialog = false
                            adminContentViewModel.addCategory(categoryNameInput, order)
                            categoryNameInput = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Category Dialog
    if (selectedCategoryForEdit != null) {
        val cat = selectedCategoryForEdit!!
        AlertDialog(
            onDismissRequest = { selectedCategoryForEdit = null },
            title = { Text("Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editCategoryNameInput,
                        onValueChange = { editCategoryNameInput = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editCategoryOrderInput,
                        onValueChange = { editCategoryOrderInput = it },
                        label = { Text("Order Index") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val order = editCategoryOrderInput.toIntOrNull() ?: cat.order
                        if (editCategoryNameInput.isNotBlank()) {
                            val targetId = cat.id
                            selectedCategoryForEdit = null
                            adminContentViewModel.updateCategory(targetId, editCategoryNameInput, order)
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryForEdit = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Skill Dialog
    if (skillToDelete != null) {
        val target = skillToDelete!!
        AlertDialog(
            onDismissRequest = { skillToDelete = null },
            title = { Text("Delete Skill Listing?") },
            text = { Text("Permanently delete '${target.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = target.id
                        skillToDelete = null
                        adminContentViewModel.deleteSkill(targetId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { skillToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Review Dialog
    if (reviewToDelete != null) {
        val target = reviewToDelete!!
        AlertDialog(
            onDismissRequest = { reviewToDelete = null },
            title = { Text("Delete Review?") },
            text = { Text("Delete this review rating (${target.rating} ★)?") },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = target.id
                        reviewToDelete = null
                        adminContentViewModel.deleteReview(targetId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Category Dialog
    if (categoryToDelete != null) {
        val target = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category?") },
            text = { Text("Delete category '${target.name}'? (Deletion is blocked if skills are listed under this category).") },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = target.id
                        val targetName = target.name
                        categoryToDelete = null
                        adminContentViewModel.deleteCategory(targetId, targetName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Content Moderation", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { adminContentViewModel.loadContent() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
        ) {
            // Section Tab Row (0 = Skills, 1 = Reviews, 2 = Categories)
            TabRow(selectedTabIndex = uiState.selectedSection) {
                Tab(selected = uiState.selectedSection == 0, onClick = { adminContentViewModel.selectSection(0) }, text = { Text("Skills (${uiState.skills.size})") })
                Tab(selected = uiState.selectedSection == 1, onClick = { adminContentViewModel.selectSection(1) }, text = { Text("Reviews (${uiState.reviews.size})") })
                Tab(selected = uiState.selectedSection == 2, onClick = { adminContentViewModel.selectSection(2) }, text = { Text("Categories (${uiState.categories.size})") })
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (uiState.selectedSection) {
                0 -> {
                    // Skills Management Section
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { adminContentViewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search skills...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Visible", "Hidden").forEach { filter ->
                            FilterChip(
                                selected = uiState.hiddenFilter == filter,
                                onClick = { adminContentViewModel.updateHiddenFilter(filter) },
                                label = { Text(filter) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredSkills = remember(uiState.skills, uiState.searchQuery, uiState.hiddenFilter) {
                        uiState.skills.filter { skill ->
                            val matchesSearch = uiState.searchQuery.isBlank() ||
                                    skill.title.contains(uiState.searchQuery, ignoreCase = true) ||
                                    skill.description.contains(uiState.searchQuery, ignoreCase = true)

                            val matchesHidden = when (uiState.hiddenFilter) {
                                "Visible" -> !skill.isHidden
                                "Hidden" -> skill.isHidden
                                else -> true
                            }

                            matchesSearch && matchesHidden
                        }
                    }

                    if (filteredSkills.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No skills found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredSkills, key = { it.id }) { skill ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(skill.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            Row {
                                                IconButton(onClick = { adminContentViewModel.toggleSkillHidden(skill.id, skill.isHidden) }) {
                                                    Icon(
                                                        imageVector = if (skill.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = "Toggle Hidden",
                                                        tint = if (skill.isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                IconButton(onClick = { skillToDelete = skill }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Skill", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                        Text("${skill.category} · Teacher: ${skill.teacherName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Reviews Management Section
                    if (uiState.reviews.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No reviews found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.reviews, key = { it.id }) { review ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${review.rating} ★", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            if (review.comment.isNotBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                                            Text("Reviewer: ${review.reviewerId} → Reviewee: ${review.revieweeId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            Text(dateFormat.format(Date(review.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        IconButton(onClick = { reviewToDelete = review }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Review", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Categories Management Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category List (${uiState.categories.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            categoryNameInput = ""
                            categoryOrderInput = "${uiState.categories.size + 1}"
                            showAddCategoryDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Category")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val sortedCats = remember(uiState.categories) { uiState.categories.sortedBy { it.order } }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sortedCats, key = { it.id }) { cat ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Order index: ${cat.order}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            selectedCategoryForEdit = cat
                                            editCategoryNameInput = cat.name
                                            editCategoryOrderInput = cat.order.toString()
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Category")
                                        }
                                        IconButton(onClick = { categoryToDelete = cat }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
