package com.skillswap.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.ui.viewmodel.CategoryViewModel
import com.skillswap.app.ui.viewmodel.FavoriteViewModel

@Composable
fun BrowseScreen(
    onSelectSkill: (SkillListing) -> Unit = {},
    listings: List<SkillListing> = emptyList(),
    currentUserId: String = "",
    categoryViewModel: CategoryViewModel = viewModel(),
    favoriteViewModel: FavoriteViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedType by remember { mutableStateOf("All") } // All, Offers, Requests

    val categoriesState by categoryViewModel.categories.collectAsState()
    val favoritesState by favoriteViewModel.favorites.collectAsState()

    val categoryNames = remember(categoriesState) {
        listOf("All") + categoriesState.map { it.name }
    }

    val favoriteSkillIds = remember(favoritesState) {
        favoritesState.map { it.skillId }.toSet()
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            favoriteViewModel.loadFavorites(currentUserId)
        }
    }

    val filteredListings = listings.filter { listing ->
        val matchesCategory = selectedCategory == "All" || listing.category == selectedCategory
        val matchesType = when (selectedType) {
            "Offers" -> listing.type == "offer"
            "Requests" -> listing.type == "request"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                listing.title.contains(searchQuery, ignoreCase = true) ||
                listing.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesType && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Browse Community Skills",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by skill or keyword...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Type Filter Segmented Buttons (All / Offers / Requests)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Offers", "Requests").forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoryNames) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Listings Feed
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredListings, key = { it.id }) { listing ->
                val isFavorite = favoriteSkillIds.contains(listing.id)
                SkillListingCard(
                    listing = listing,
                    isFavorite = isFavorite,
                    onClick = { onSelectSkill(listing) },
                    onFavoriteToggle = {
                        if (currentUserId.isNotBlank()) {
                            favoriteViewModel.toggleFavorite(currentUserId, listing.id)
                        }
                    }
                )
            }
        }
    }
}
