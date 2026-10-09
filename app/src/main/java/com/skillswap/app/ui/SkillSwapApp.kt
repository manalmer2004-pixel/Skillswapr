package com.skillswap.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.skillswap.app.data.MockData
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.ui.screens.*
import com.skillswap.app.ui.viewmodel.AuthViewModel

@Composable
fun SkillSwapApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authUiState by authViewModel.uiState.collectAsState()
    val startDest = if (authUiState.isAuthenticated) "explore" else "welcome"
    var currentRoute by remember(startDest) { mutableStateOf(startDest) }
    var selectedListingDetail by remember { mutableStateOf<SkillListing?>(null) }

    Scaffold(
        bottomBar = {
            if (currentRoute in listOf("explore", "browse", "chat", "profile")) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") },
                        selected = currentRoute == "explore",
                        onClick = {
                            currentRoute = "explore"
                            navController.navigate("explore") { popUpTo("explore") { inclusive = true } }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Layers, contentDescription = "Browse") },
                        label = { Text("Browse") },
                        selected = currentRoute == "browse",
                        onClick = {
                            currentRoute = "browse"
                            navController.navigate("browse") { popUpTo("browse") { inclusive = true } }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                        label = { Text("Chat") },
                        selected = currentRoute == "chat",
                        onClick = {
                            currentRoute = "chat"
                            navController.navigate("chat") { popUpTo("chat") { inclusive = true } }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentRoute == "profile",
                        onClick = {
                            currentRoute = "profile"
                            navController.navigate("profile") { popUpTo("profile") { inclusive = true } }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute in listOf("explore", "browse")) {
                FloatingActionButton(
                    onClick = {
                        currentRoute = "create_listing"
                        navController.navigate("create_listing")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post Skill")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("welcome") {
                WelcomeScreen(
                    onGetStarted = {
                        currentRoute = "auth"
                        navController.navigate("auth")
                    },
                    onSignIn = {
                        currentRoute = "auth"
                        navController.navigate("auth")
                    }
                )
            }
            composable("auth") {
                AuthScreen(
                    onAuthenticated = {
                        currentRoute = "explore"
                        navController.navigate("explore") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    },
                    onBackToWelcome = {
                        currentRoute = "welcome"
                        navController.navigate("welcome")
                    },
                    viewModel = authViewModel
                )
            }
            composable("explore") {
                ExploreScreen(
                    onSelectSkill = { listing ->
                        selectedListingDetail = listing
                        currentRoute = "listing_detail"
                        navController.navigate("listing_detail")
                    },
                    onRequestSkill = { listing ->
                        selectedListingDetail = listing
                        currentRoute = "listing_detail"
                        navController.navigate("listing_detail")
                    }
                )
            }
            composable("browse") {
                BrowseScreen(
                    onSelectSkill = { listing ->
                        selectedListingDetail = listing
                        currentRoute = "listing_detail"
                        navController.navigate("listing_detail")
                    }
                )
            }
            composable("chat") {
                ChatScreen()
            }
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        currentRoute = "welcome"
                        navController.navigate("welcome") {
                            popUpTo(0)
                        }
                    },
                    authViewModel = authViewModel
                )
            }
            composable("create_listing") {
                CreateListingScreen(
                    onDismiss = {
                        currentRoute = "explore"
                        navController.popBackStack()
                    },
                    onPublish = { newListing ->
                        currentRoute = "explore"
                        navController.popBackStack()
                    }
                )
            }
            composable("listing_detail") {
                val listing = selectedListingDetail ?: MockData.INITIAL_LISTINGS.first()
                ListingDetailScreen(
                    listing = listing,
                    onBack = {
                        currentRoute = "explore"
                        navController.popBackStack()
                    },
                    onRequestSwap = {
                        currentRoute = "chat"
                        navController.navigate("chat") {
                            popUpTo("explore")
                        }
                    }
                )
            }
        }
    }
}
