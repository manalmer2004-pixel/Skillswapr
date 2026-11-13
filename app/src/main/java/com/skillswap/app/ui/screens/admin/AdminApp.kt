package com.skillswap.app.ui.screens.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun AdminApp(
    onSwitchToUserView: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "admin_overview"

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
                    label = { Text("Overview") },
                    selected = currentRoute == "admin_overview",
                    onClick = {
                        navController.navigate("admin_overview") {
                            popUpTo("admin_overview") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, contentDescription = "Users") },
                    label = { Text("Users") },
                    selected = currentRoute == "admin_users",
                    onClick = {
                        navController.navigate("admin_users") {
                            popUpTo("admin_overview") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Content") },
                    label = { Text("Content") },
                    selected = currentRoute == "admin_content",
                    onClick = {
                        navController.navigate("admin_content") {
                            popUpTo("admin_overview") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Activity") },
                    label = { Text("Activity") },
                    selected = currentRoute == "admin_activity",
                    onClick = {
                        navController.navigate("admin_activity") {
                            popUpTo("admin_overview") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text("More") },
                    selected = currentRoute == "admin_more",
                    onClick = {
                        navController.navigate("admin_more") {
                            popUpTo("admin_overview") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "admin_overview",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("admin_overview") {
                AdminOverviewScreen()
            }
            composable("admin_users") {
                AdminUsersScreen()
            }
            composable("admin_content") {
                AdminContentScreen()
            }
            composable("admin_activity") {
                AdminActivityScreen()
            }
            composable("admin_more") {
                AdminMoreScreen(
                    onSwitchToUserView = onSwitchToUserView,
                    onLogout = onLogout
                )
            }
        }
    }
}
