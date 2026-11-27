package com.skillswap.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skillswap.app.data.model.UserProfile
import com.skillswap.app.ui.viewmodel.AdminUserDetailData
import com.skillswap.app.ui.viewmodel.AdminUsersViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailScreen(
    detailData: AdminUserDetailData,
    currentAdminId: String,
    onBack: () -> Unit,
    adminUsersViewModel: AdminUsersViewModel
) {
    val user = detailData.user
    val isSelf = currentAdminId.isNotBlank() && currentAdminId == user.id
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }

    var showSuspendDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var showCreditAdjustDialog by remember { mutableStateOf(false) }

    var creditAmountInput by remember { mutableStateOf("") }
    var creditReasonInput by remember { mutableStateOf("") }
    var creditAdjustError by remember { mutableStateOf<String?>(null) }

    // Suspend / Reactivate Confirmation Dialog
    if (showSuspendDialog) {
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            title = { Text(if (user.isSuspended) "Reactivate User?" else "Suspend User?") },
            text = {
                Text(
                    if (user.isSuspended) "Are you sure you want to lift the suspension for '${user.name}'?"
                    else "Are you sure you want to suspend '${user.name}'? They will be logged out and blocked from signing in."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuspendDialog = false
                        adminUsersViewModel.suspendUser(user.id, currentAdminId, !user.isSuspended)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.isSuspended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (user.isSuspended) "Reactivate" else "Suspend")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuspendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Verify / Unverify Confirmation Dialog
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text(if (user.isVerified) "Remove Verification?" else "Verify User Account?") },
            text = {
                Text(
                    if (user.isVerified) "Remove verified status badge for '${user.name}'?"
                    else "Grant verified status badge for '${user.name}'?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVerifyDialog = false
                        adminUsersViewModel.verifyUser(user.id, currentAdminId, !user.isVerified)
                    }
                ) {
                    Text(if (user.isVerified) "Remove Badge" else "Grant Verification")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Credit Adjustment Dialog
    if (showCreditAdjustDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreditAdjustDialog = false
                creditAdjustError = null
            },
            title = { Text("Adjust Time Credits") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "User: ${user.name} · Current Balance: ${user.timeCreditBalance} hrs",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = creditAmountInput,
                        onValueChange = { creditAmountInput = it },
                        label = { Text("Adjustment Amount (+ / -)") },
                        placeholder = { Text("e.g. 2.0 or -1.5") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = creditReasonInput,
                        onValueChange = { creditReasonInput = it },
                        label = { Text("Reason for Adjustment (Required)") },
                        placeholder = { Text("e.g. Bonus reward for community service") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    creditAdjustError?.let { err ->
                        Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = creditAmountInput.toDoubleOrNull()
                        if (amount == null || amount == 0.0) {
                            creditAdjustError = "Please enter a valid non-zero number"
                            return@Button
                        }
                        if (creditReasonInput.isBlank()) {
                            creditAdjustError = "Reason is required for credit adjustments"
                            return@Button
                        }
                        if (user.timeCreditBalance + amount < 0.0) {
                            creditAdjustError = "Resulting balance cannot be negative"
                            return@Button
                        }

                        creditAdjustError = null
                        showCreditAdjustDialog = false
                        adminUsersViewModel.adjustUserCredits(user.id, currentAdminId, amount, creditReasonInput)
                    }
                ) {
                    Text("Apply Adjustment")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreditAdjustDialog = false
                        creditAdjustError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.name.ifBlank { "User Details" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.name.take(1).ifBlank { "U" },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${user.email} · ${user.handle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Tags
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (user.isSuspended) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "SUSPENDED",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            if (user.isVerified) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "VERIFIED",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (user.isAdmin) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ADMINISTRATOR",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Time Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${user.timeCreditBalance} hrs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Average Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (detailData.reviewCount > 0) String.format(Locale.US, "%.1f ★ (%d)", detailData.rating, detailData.reviewCount) else "New Member",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Admin Action Buttons
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Admin Account Controls",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (isSelf) {
                            Text(
                                text = "🔒 You cannot edit or adjust your own admin account settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showSuspendDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (user.isSuspended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (user.isSuspended) "Reactivate" else "Suspend")
                                }

                                OutlinedButton(
                                    onClick = { showVerifyDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (user.isVerified) "Unverify" else "Verify")
                                }
                            }

                            Button(
                                onClick = {
                                    creditAmountInput = ""
                                    creditReasonInput = ""
                                    creditAdjustError = null
                                    showCreditAdjustDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Adjust Time Credits")
                            }
                        }
                    }
                }
            }

            // User Listed Skills
            item {
                Text("Listed Skills (${detailData.skills.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (detailData.skills.isEmpty()) {
                item {
                    Text("No skills posted by this user.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                itemsIndexed(
                    items = detailData.skills,
                    key = { index, skill -> "${skill.id}_$index" }
                ) { _, skill ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(skill.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (skill.isHidden) {
                                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(4.dp)) {
                                        Text("HIDDEN", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                            Text("${skill.category} · ${skill.creditCost} credits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // User Transactions History
            item {
                Text("Transaction Ledger (${detailData.transactions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (detailData.transactions.isEmpty()) {
                item {
                    Text("No credit transactions recorded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                itemsIndexed(
                    items = detailData.transactions,
                    key = { index, tx -> "${tx.id}_$index" }
                ) { _, tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tx.type.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                if (tx.reason.isNotBlank()) Text("Reason: ${tx.reason}", style = MaterialTheme.typography.bodySmall)
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
