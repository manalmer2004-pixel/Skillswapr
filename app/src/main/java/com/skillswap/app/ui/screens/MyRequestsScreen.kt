package com.skillswap.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.app.data.model.Review
import com.skillswap.app.data.model.SwapRequest
import com.skillswap.app.data.model.SwapRequestStatus
import com.skillswap.app.ui.viewmodel.AuthViewModel
import com.skillswap.app.ui.viewmodel.ReviewViewModel
import com.skillswap.app.ui.viewmodel.SwapRequestViewModel

@Composable
fun MyRequestsScreen(
    authViewModel: AuthViewModel = viewModel(),
    requestViewModel: SwapRequestViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel(),
    onNavigateToChat: () -> Unit = {}
) {
    val authState by authViewModel.uiState.collectAsState()
    val userId = authState.userProfile?.id ?: ""

    val sentRequests by requestViewModel.requests.collectAsState()
    val receivedRequests by requestViewModel.receivedRequests.collectAsState()
    val actionResult by requestViewModel.actionResult.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sent, 1 = Received
    val snackbarHostState = remember { SnackbarHostState() }

    var requestToReview by remember { mutableStateOf<SwapRequest?>(null) }
    var reviewRating by remember { mutableDoubleStateOf(5.0) }
    var reviewComment by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            requestViewModel.loadRequests(userId)
        }
    }

    LaunchedEffect(actionResult) {
        actionResult?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            requestViewModel.clearActionResult()
        }
    }

    // Review Dialog
    if (requestToReview != null) {
        val target = requestToReview!!
        val isRequester = userId == target.requesterId
        val revieweeId = if (isRequester) target.providerId else target.requesterId

        AlertDialog(
            onDismissRequest = { requestToReview = null },
            title = { Text("Leave Review for ${target.skillTitle}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Rating (1 - 5 Stars):")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { reviewRating = star.toDouble() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "$star Stars",
                                    tint = if (star <= reviewRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("Comment / Feedback") },
                        placeholder = { Text("Share how the skill exchange went...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val review = Review(
                            sessionId = target.id,
                            reviewerId = userId,
                            revieweeId = revieweeId,
                            rating = reviewRating,
                            comment = reviewComment
                        )
                        reviewViewModel.submitReview(review) {
                            requestToReview = null
                        }
                    }
                ) {
                    Text("Submit Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToReview = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Swap Requests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sent (${sentRequests.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Received (${receivedRequests.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val currentList = if (selectedTab == 0) sentRequests else receivedRequests

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "No sent requests yet" else "No received requests yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.id }) { request ->
                        SwapRequestCard(
                            request = request,
                            isReceived = selectedTab == 1,
                            onUpdateStatus = { status ->
                                requestViewModel.updateStatus(request.id, status)
                            },
                            onOpenReview = {
                                requestToReview = request
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwapRequestCard(
    request: SwapRequest,
    isReceived: Boolean,
    onUpdateStatus: (SwapRequestStatus) -> Unit,
    onOpenReview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.skillTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = when (request.status) {
                        SwapRequestStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                        SwapRequestStatus.ACCEPTED -> MaterialTheme.colorScheme.primaryContainer
                        SwapRequestStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                        SwapRequestStatus.DECLINED, SwapRequestStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = request.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Provider: ${request.providerName} · Duration: ${request.durationHours} hrs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (request.status == SwapRequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                if (isReceived) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onUpdateStatus(SwapRequestStatus.ACCEPTED) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Accept")
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus(SwapRequestStatus.DECLINED) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Decline")
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus(SwapRequestStatus.CANCELLED) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { onUpdateStatus(SwapRequestStatus.CANCELLED) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Request")
                    }
                }
            } else if (request.status == SwapRequestStatus.ACCEPTED && isReceived) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onUpdateStatus(SwapRequestStatus.COMPLETED) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Mark Completed (Transfer Credits)")
                }
            } else if (request.status == SwapRequestStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenReview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Leave Review")
                }
            }
        }
    }
}
