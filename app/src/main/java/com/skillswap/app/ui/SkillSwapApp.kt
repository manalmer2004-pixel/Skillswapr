package com.skillswap.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.data.repository.CategoryRepository
import com.skillswap.app.data.repository.SkillRepository
import com.skillswap.app.ui.screens.*
import com.skillswap.app.ui.viewmodel.AuthViewModel
import com.skillswap.app.ui.viewmodel.ChatViewModel
import com.skillswap.app.ui.viewmodel.ReviewViewModel
import kotlinx.coroutines.launch

@Composable
fun SkillSwapApp(
    authViewModel: AuthViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUserId = authUiState.userProfile?.id ?: ""
    val startDest = if (authUiState.isAuthenticated) "explore" else "welcome"
    var selectedListingDetail by remember { mutableStateOf<SkillListing?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabRoutes = listOf("explore", "browse", "map", "chat", "profile")

    fun goToTab(route: String) {
        navController.navigate(route) {
            popUpTo("explore") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val scope = rememberCoroutineScope()
    val skillRepository = remember { SkillRepository() }
    val chatViewModel: ChatViewModel = viewModel()
    val allListings = remember { mutableStateListOf<SkillListing>() }

    val ratingSummaries by reviewViewModel.ratingSummaries.collectAsState()

    val isAdmin = authUiState.userProfile?.isAdmin == true
    LaunchedEffect(isAdmin) {
        if (isAdmin) CategoryRepository().seedIfEmpty()
    }

    LaunchedEffect(Unit) {
        val loadedSkills = skillRepository.getSkills()
        allListings.clear()
        allListings.addAll(loadedSkills)
    }

    LaunchedEffect(allListings.size) {
        reviewViewModel.loadRatingSummaries(allListings.map { it.userId }.distinct())
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in tabRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") },
                        selected = currentRoute == "explore",
                        onClick = { goToTab("explore") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Layers, contentDescription = "Browse") },
                        label = { Text("Browse") },
                        selected = currentRoute == "browse",
                        onClick = { goToTab("browse") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                        label = { Text("Map") },
                        selected = currentRoute == "map",
                        onClick = { goToTab("map") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                        label = { Text("Chat") },
                        selected = currentRoute == "chat",
                        onClick = { goToTab("chat") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentRoute == "profile",
                        onClick = { goToTab("profile") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute in listOf("explore", "browse")) {
                FloatingActionButton(
                    onClick = {
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
                        navController.navigate("auth")
                    },
                    onSignIn = {
                        navController.navigate("auth")
                    }
                )
            }
            composable("auth") {
                AuthScreen(
                    onAuthenticated = {
                        navController.navigate("explore") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    },
                    onBackToWelcome = {
                        navController.navigate("welcome")
                    },
                    viewModel = authViewModel
                )
            }
            composable("explore") {
                ExploreScreen(
                    onSelectSkill = { listing ->
                        selectedListingDetail = listing
                        navController.navigate("listing_detail")
                    },
                    onRequestSkill = { listing ->
                        selectedListingDetail = listing
                        navController.navigate("listing_detail")
                    },
                    onNavigateToMap = {
                        goToTab("map")
                    },
                    listings = allListings,
                    currentUserId = currentUserId,
                    ratingSummaries = ratingSummaries
                )
            }
            composable("browse") {
                BrowseScreen(
                    onSelectSkill = { listing ->
                        selectedListingDetail = listing
                        navController.navigate("listing_detail")
                    },
                    onNavigateToMap = {
                        goToTab("map")
                    },
                    listings = allListings,
                    currentUserId = currentUserId,
                    ratingSummaries = ratingSummaries
                )
            }
            composable("map") {
                MapScreen(
                    onSelectSkill = { listing ->
                        selectedListingDetail = listing
                        navController.navigate("listing_detail")
                    },
                    onNavigateToList = {
                        goToTab("explore")
                    },
                    authViewModel = authViewModel
                )
            }
            composable("chat") {
                ChatScreen(chatViewModel = chatViewModel, authViewModel = authViewModel)
            }
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        navController.navigate("welcome") {
                            popUpTo(0)
                        }
                    },
                    onNavigateToRequests = {
                        navController.navigate("my_requests")
                    },
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToTransactions = {
                        navController.navigate("transactions")
                    },
                    authViewModel = authViewModel,
                    reviewViewModel = reviewViewModel,
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
                    reviewViewModel = reviewViewModel,
                    onNavigateToChat = {
                        goToTab("chat")
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
                        navController.popBackStack()
                    },
                    onPublish = { newListing ->
                        allListings.add(0, newListing)
                        scope.launch {
                            skillRepository.addSkill(newListing)
                        }
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
                        navController.popBackStack()
                    },
                    onNavigateToRequests = {
                        navController.navigate("my_requests")
                    },
                    onStartChat = { otherUserId ->
                        chatViewModel.startChatWithUser(currentUserId, otherUserId) {
                            goToTab("chat")
                        }
                    },
                    reviewViewModel = reviewViewModel
                )
            }
        }
    }
}
