package com.skillswap.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.app.data.model.SwapRequest
import com.skillswap.app.data.model.SwapRequestStatus
import com.skillswap.app.data.model.TransactionType
import com.skillswap.app.ui.viewmodel.AdminActivityViewModel
import com.skillswap.app.ui.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActivityScreen(
    authViewModel: AuthViewModel = viewModel(),
    adminActivityViewModel: AdminActivityViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val currentAdminId = authState.userProfile?.id ?: ""

    val uiState by adminActivityViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var requestToCancel by remember { mutableStateOf<SwapRequest?>(null) }
    var cancelReasonInput by remember { mutableStateOf("") }
    var cancelReasonError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminActivityViewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminActivityViewModel.clearMessages()
        }
    }

    // Force Cancel Dialog
    if (requestToCancel != null) {
        val target = requestToCancel!!
        AlertDialog(
            onDismissRequest = {
                requestToCancel = null
                cancelReasonError = null
            },
            title = { Text("Force Cancel Swap Request?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Swap: ${target.skillTitle} · Status: ${target.status.name}")
                    OutlinedTextField(
                        value = cancelReasonInput,
                        onValueChange = { cancelReasonInput = it },
                        label = { Text("Reason for Cancellation (Required)") },
                        placeholder = { Text("e.g. Inappropriate request details / Policy violation") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    cancelReasonError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelReasonInput.isBlank()) {
                            cancelReasonError = "Reason is required for force cancellation"
                            return@Button
                        }
                        val targetId = target.id
                        requestToCancel = null
                        cancelReasonError = null
                        adminActivityViewModel.forceCancelSwapRequest(targetId, currentAdminId, cancelReasonInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Force Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    requestToCancel = null
                    cancelReasonError = null
                }) { Text("Dismiss") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Activity & Ledger", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { adminActivityViewModel.loadActivity() }) {
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
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(selected = uiState.selectedTab == 0, onClick = { adminActivityViewModel.selectTab(0) }, text = { Text("Swap Requests (${uiState.swapRequests.size})") })
                Tab(selected = uiState.selectedTab == 1, onClick = { adminActivityViewModel.selectTab(1) }, text = { Text("Credit Ledger (${uiState.transactions.size})") })
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (uiState.selectedTab) {
                0 -> {
                    // Swap Requests Tab
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "PENDING", "ACCEPTED", "COMPLETED", "DECLINED", "CANCELLED")) { status ->
                            FilterChip(
                                selected = uiState.swapStatusFilter == status,
                                onClick = { adminActivityViewModel.updateSwapStatusFilter(status) },
                                label = { Text(status) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredSwaps = remember(uiState.swapRequests, uiState.swapStatusFilter) {
                        if (uiState.swapStatusFilter == "All") uiState.swapRequests
                        else uiState.swapRequests.filter { it.status.name == uiState.swapStatusFilter }
                    }

                    if (filteredSwaps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No swap requests found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredSwaps, key = { it.id }) { req ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(req.skillTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            Surface(
                                                color = when (req.status) {
                                                    SwapRequestStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer
                                                    SwapRequestStatus.ACCEPTED -> MaterialTheme.colorScheme.tertiaryContainer
                                                    SwapRequestStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                                                    SwapRequestStatus.DECLINED, SwapRequestStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(req.status.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Text("Requester: ${req.requesterId} → Provider: ${req.providerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Time: ${req.proposedTime} (${req.durationHours} hrs)", style = MaterialTheme.typography.bodySmall)
                                        Text(dateFormat.format(Date(req.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                                        if (req.status == SwapRequestStatus.PENDING || req.status == SwapRequestStatus.ACCEPTED) {
                                            OutlinedButton(
                                                onClick = {
                                                    cancelReasonInput = ""
                                                    cancelReasonError = null
                                                    requestToCancel = req
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Force Cancel")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Credit Ledger & Anomalies Tab
                    if (uiState.anomalies.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Detected Anomalies (${uiState.anomalies.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                                uiState.anomalies.forEach { anomaly ->
                                    Text("• ${anomaly.title}: ${anomaly.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { adminActivityViewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search by user or transaction ID...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "EARNED", "SPENT", "BONUS", "ADMIN_ADJUSTMENT")) { type ->
                            FilterChip(
                                selected = uiState.txTypeFilter == type,
                                onClick = { adminActivityViewModel.updateTxTypeFilter(type) },
                                label = { Text(type) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredTxs = remember(uiState.transactions, uiState.searchQuery, uiState.txTypeFilter) {
                        uiState.transactions.filter { tx ->
                            val matchesSearch = uiState.searchQuery.isBlank() ||
                                    tx.userId.contains(uiState.searchQuery, ignoreCase = true) ||
                                    tx.id.contains(uiState.searchQuery, ignoreCase = true)

                            val matchesType = if (uiState.txTypeFilter == "All") true
                            else tx.type.name == uiState.txTypeFilter

                            matchesSearch && matchesType
                        }
                    }

                    if (filteredTxs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No credit transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredTxs, key = { it.id }) { tx ->
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${tx.type.name} · ${tx.userId.take(8)}...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            if (tx.reason.isNotBlank()) Text("Reason: ${tx.reason}", style = MaterialTheme.typography.bodySmall)
                                            Text("Balance after: ${tx.balanceAfter} credits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(dateFormat.format(Date(tx.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text(
                                            text = if (tx.amount >= 0) "+${tx.amount} hrs" else "${tx.amount} hrs",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
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
