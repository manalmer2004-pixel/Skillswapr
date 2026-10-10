package com.skillswap.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.data.repository.SkillRepository
import com.skillswap.app.ui.screens.*
import com.skillswap.app.ui.viewmodel.AuthViewModel
import com.skillswap.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun SkillSwapApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUserId = authUiState.userProfile?.id ?: ""
    val startDest = if (authUiState.isAuthenticated) "explore" else "welcome"
    var currentRoute by remember(startDest) { mutableStateOf(startDest) }
    var selectedListingDetail by remember { mutableStateOf<SkillListing?>(null) }

    val scope = rememberCoroutineScope()
    val skillRepository = remember { SkillRepository() }
    val chatViewModel: ChatViewModel = viewModel()
    val allListings = remember { mutableStateListOf<SkillListing>() }

    LaunchedEffect(Unit) {
        val loadedSkills = skillRepository.getSkills()
        allListings.clear()
        allListings.addAll(loadedSkills)
    }

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
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
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
                    },
                    listings = allListings,
                    currentUserId = currentUserId
                )
            }
            composable("browse") {
                BrowseScreen(
                    onSelectSkill = { listing ->
                        selectedListingDetail = listing
                        currentRoute = "listing_detail"
                        navController.navigate("listing_detail")
                    },
                    listings = allListings,
                    currentUserId = currentUserId
                )
            }
            composable("chat") {
                ChatScreen(chatViewModel = chatViewModel, authViewModel = authViewModel)
            }
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        currentRoute = "welcome"
                        navController.navigate("welcome") {
                            popUpTo(0)
                        }
                    },
                    onNavigateToRequests = {
                        currentRoute = "my_requests"
                        navController.navigate("my_requests")
                    },
                    onNavigateToNotifications = {
                        currentRoute = "notifications"
                        navController.navigate("notifications")
                    },
                    onNavigateToTransactions = {
                        currentRoute = "transactions"
                        navController.navigate("transactions")
                    },
                    authViewModel = authViewModel,
                    userListings = allListings,
                    onDeleteListing = { listingId ->
                        allListings.removeAll { it.id == listingId }
                        scope.launch {
                            skillRepository.deleteSkill(listingId)
                        }
                    }
                )
            }
            composable("my_requests") {
                MyRequestsScreen(
                    authViewModel = authViewModel,
                    onNavigateToChat = {
                        currentRoute = "chat"
                        navController.navigate("chat")
                    }
                )
            }
            composable("notifications") {
                NotificationsScreen(authViewModel = authViewModel)
            }
            composable("transactions") {
                TransactionHistoryScreen(authViewModel = authViewModel)
            }
            composable("create_listing") {
                val currentUserName = authUiState.userProfile?.name ?: ""

                CreateListingScreen(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    onDismiss = {
                        currentRoute = "explore"
                        navController.popBackStack()
                    },
                    onPublish = { newListing ->
                        allListings.add(0, newListing)
                        scope.launch {
                            skillRepository.addSkill(newListing)
                        }
                        currentRoute = "explore"
                        navController.popBackStack()
                    }
                )
            }
            composable("listing_detail") {
                val listing = selectedListingDetail ?: allListings.firstOrNull() ?: SkillListing()
                ListingDetailScreen(
                    listing = listing,
                    currentUserId = currentUserId,
                    onBack = {
                        currentRoute = "explore"
                        navController.popBackStack()
                    },
                    onNavigateToRequests = {
                        currentRoute = "my_requests"
                        navController.navigate("my_requests")
                    },
                    onStartChat = { otherUserId ->
                        chatViewModel.startChatWithUser(currentUserId, otherUserId) {
                            currentRoute = "chat"
                            navController.navigate("chat")
                        }
                    }
                )
            }
        }
    }
}
