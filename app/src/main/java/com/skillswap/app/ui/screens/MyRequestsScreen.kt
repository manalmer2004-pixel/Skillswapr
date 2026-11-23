package com.skillswap.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.skillswap.app.data.model.Review
import com.skillswap.app.data.model.SwapRequest
import com.skillswap.app.data.model.SwapRequestStatus
import com.skillswap.app.ui.viewmodel.AuthViewModel
import com.skillswap.app.ui.viewmodel.ReviewViewModel
import com.skillswap.app.ui.viewmodel.SwapRequestViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    authViewModel: AuthViewModel = viewModel(),
    requestViewModel: SwapRequestViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel(),
    onNavigateToChat: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val authState by authViewModel.uiState.collectAsState()
    val userId = authState.userProfile?.id ?: ""

    val sentRequests by requestViewModel.requests.collectAsState()
    val receivedRequests by requestViewModel.receivedRequests.collectAsState()
    val actionResult by requestViewModel.actionResult.collectAsState()

    val isSubmitting by reviewViewModel.isSubmitting.collectAsState()
    val reviewResultMessage by reviewViewModel.resultMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sent, 1 = Received
    val snackbarHostState = remember { SnackbarHostState() }

    var dismissedRequestIds by remember { mutableStateOf(setOf<String>()) }
    var reviewedRequestIds by remember { mutableStateOf(setOf<String>()) }

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

    // Failure message handler for review submission
    LaunchedEffect(reviewResultMessage) {
        reviewResultMessage?.let { msg ->
            if (msg != "Review submitted successfully") {
                snackbarHostState.showSnackbar(msg)
                reviewViewModel.clearResultMessage()
            }
        }
    }

    // Auto-popup logic for learner (requester in sentRequests)
    LaunchedEffect(sentRequests, userId, dismissedRequestIds, reviewedRequestIds, requestToReview) {
        if (userId.isNotBlank() && requestToReview == null) {
            val unreviewedSentCompleted = sentRequests.filter { req ->
                req.status == SwapRequestStatus.COMPLETED &&
                        !dismissedRequestIds.contains(req.id) &&
                        !reviewedRequestIds.contains(req.id)
            }

            for (req in unreviewedSentCompleted) {
                try {
                    val docId = "${req.id}_${userId}"
                    val doc = FirebaseFirestore.getInstance().collection("reviews").document(docId).get().await()
                    if (doc.exists()) {
                        reviewedRequestIds = reviewedRequestIds + req.id
                    } else {
                        reviewRating = 5.0
                        reviewComment = ""
                        requestToReview = req
                        break
                    }
                } catch (_: Exception) {
                    // Ignore check failure
                }
            }
        }
    }

    // Review Dialog
    if (requestToReview != null) {
        val target = requestToReview!!
        val isRequester = userId == target.requesterId
        val revieweeId = if (isRequester) target.providerId else target.requesterId
        val providerName = if (isRequester) target.providerName.ifBlank { "Provider" } else "User"

        val ratingLabel = when (reviewRating.toInt()) {
            1 -> "Poor"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Very good"
            5 -> "Excellent"
            else -> "Excellent"
        }

        AlertDialog(
            onDismissRequest = {
                dismissedRequestIds = dismissedRequestIds + target.id
                requestToReview = null
            },
            title = {
                Column {
                    Text(
                        text = if (isRequester) "Rate $providerName" else "Leave Review for ${target.skillTitle}",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "How was your session for ${target.skillTitle}?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered 40.dp stars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { reviewRating = star.toDouble() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$star Stars",
                                    modifier = Modifier.size(40.dp),
                                    tint = if (star <= reviewRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Text(
                        text = ratingLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("Comment / Feedback (Optional)") },
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
                            reviewedRequestIds = reviewedRequestIds + target.id
                            requestToReview = null
                            reviewViewModel.clearResultMessage()
                            scope.launch {
                                snackbarHostState.showSnackbar("Thanks for rating $providerName!")
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit Review")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dismissedRequestIds = dismissedRequestIds + target.id
                        requestToReview = null
                    },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Swap Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            currentUserId = userId,
                            isReceived = selectedTab == 1,
                            isReviewedInSession = reviewedRequestIds.contains(request.id),
                            onUpdateStatus = { status ->
                                requestViewModel.updateStatus(request.id, status, userId)
                            },
                            onOpenReview = {
                                reviewRating = 5.0
                                reviewComment = ""
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
    currentUserId: String,
    isReceived: Boolean,
    isReviewedInSession: Boolean,
    onUpdateStatus: (SwapRequestStatus) -> Unit,
    onOpenReview: () -> Unit
) {
    var hasBeenReviewed by remember { mutableStateOf(false) }

    LaunchedEffect(request.id, currentUserId, isReviewedInSession) {
        if (isReviewedInSession) {
            hasBeenReviewed = true
        } else if (currentUserId.isNotBlank() && request.status == SwapRequestStatus.COMPLETED) {
            try {
                val docId = "${request.id}_${currentUserId}"
                val doc = FirebaseFirestore.getInstance().collection("reviews").document(docId).get().await()
                hasBeenReviewed = doc.exists()
            } catch (_: Exception) {}
        }
    }

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
                if (hasBeenReviewed) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = "✓ Reviewed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
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
}
