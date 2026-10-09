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
import com.skillswap.app.data.MockData
import com.skillswap.app.data.model.SkillListing

@Composable
fun BrowseScreen(
    onSelectSkill: (SkillListing) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedType by remember { mutableStateOf("All") } // All, Offers, Requests
    var listings by remember { mutableStateOf(MockData.INITIAL_LISTINGS) }

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
            items(MockData.SKILL_CATEGORIES) { category ->
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
                SkillListingCard(
                    listing = listing,
                    onClick = { onSelectSkill(listing) },
                    onBookmarkToggle = { target ->
                        listings = listings.map { item ->
                            if (item.id == target.id) item.copy(saved = !item.saved)
                            else item
                        }
                    }
                )
            }
        }
    }
}
