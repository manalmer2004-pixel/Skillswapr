package com.skillswap.app.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.app.ui.viewmodel.AdminOverviewViewModel
import com.skillswap.app.ui.viewmodel.RecentActivityItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOverviewScreen(
    overviewViewModel: AdminOverviewViewModel = viewModel()
) {
    val uiState by overviewViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            overviewViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Overview", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { overviewViewModel.loadOverviewData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Key Metrics Summary Cards
                item {
                    Text(
                        text = "System Overview & Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            MetricCard(
                                title = "Total Users",
                                value = "${uiState.totalUsers}",
                                subtitle = "+${uiState.newUsersLast7Days} new (7 days)",
                                iconColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        item {
                            MetricCard(
                                title = "Suspended Users",
                                value = "${uiState.suspendedUsers}",
                                subtitle = "${uiState.verifiedUsers} verified",
                                iconColor = MaterialTheme.colorScheme.error
                            )
                        }
                        item {
                            MetricCard(
                                title = "Total Skills",
                                value = "${uiState.totalSkills}",
                                subtitle = "${uiState.hiddenSkills} hidden",
                                iconColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        item {
                            MetricCard(
                                title = "Credits in Circulation",
                                value = "${uiState.totalCreditsCirculation} hrs",
                                subtitle = "${uiState.totalCreditsTransferred} hrs transferred",
                                iconColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                        item {
                            MetricCard(
                                title = "Reviews & Rating",
                                value = String.format(Locale.US, "%.1f ★", uiState.averageRating),
                                subtitle = "${uiState.totalReviews} total reviews",
                                iconColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Swaps Status Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Swap Requests Status Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatusPill("Pending", uiState.swapCountPending, MaterialTheme.colorScheme.primary)
                                StatusPill("Accepted", uiState.swapCountAccepted, MaterialTheme.colorScheme.tertiary)
                                StatusPill("Completed", uiState.swapCountCompleted, MaterialTheme.colorScheme.secondary)
                                StatusPill("Cancelled", uiState.swapCountCancelled, MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Chart 1: Signups per Day (Last 7 Days)
                item {
                    ChartCard(
                        title = "User Signups (Last 7 Days)",
                        data = uiState.signupsLast7Days,
                        barColor = MaterialTheme.colorScheme.primary
                    )
                }

                // Chart 2: Skills per Category
                item {
                    ChartCard(
                        title = "Top Categories by Skills Offered",
                        data = uiState.skillsByCategory,
                        barColor = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Chart 3: Swaps per Status
                item {
                    ChartCard(
                        title = "Swaps Distribution by Status",
                        data = uiState.swapsByStatus,
                        barColor = MaterialTheme.colorScheme.secondary
                    )
                }

                // Recent Activity Feed
                item {
                    Text(
                        text = "Recent Activity Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.recentActivity.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "No recent system activity recorded.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.recentActivity,
                        key = { index, item -> "${item.id}_$index" }
                    ) { _, item ->
                        ActivityItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    iconColor: Color
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusPill(label: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    data: List<Pair<String, Int>>,
    barColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (data.isEmpty() || data.all { it.second == 0 }) {
                Text(
                    text = "No data available for chart",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = data.maxOf { it.second }.coerceAtLeast(1)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barWidth = (canvasWidth / data.size) * 0.5f
                    val spacing = (canvasWidth / data.size)

                    data.forEachIndexed { index, pair ->
                        val barHeight = (pair.second.toFloat() / maxVal.toFloat()) * (canvasHeight - 30.dp.toPx())
                        val x = index * spacing + (spacing - barWidth) / 2f
                        val y = canvasHeight - 20.dp.toPx() - barHeight

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    data.forEach { pair ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = pair.first.take(5),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${pair.second}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItemCard(item: RecentActivityItem) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (item.type == "SWAP") Icons.Default.SwapHoriz else Icons.Default.Star,
                contentDescription = null,
                tint = if (item.type == "SWAP") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
