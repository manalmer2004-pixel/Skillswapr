package com.skillswap.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.app.data.model.CreditTransaction
import com.skillswap.app.data.model.TransactionType
import com.skillswap.app.ui.viewmodel.AuthViewModel
import com.skillswap.app.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionHistoryScreen(
    authViewModel: AuthViewModel = viewModel(),
    txViewModel: TransactionViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val userId = authState.userProfile?.id ?: "user_maya"
    val transactions by txViewModel.transactions.collectAsState()

    LaunchedEffect(userId) {
        txViewModel.loadTransactions(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Time Credit Ledger",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No credit transactions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionCard(tx = tx)
                }
            }
        }
    }
}

@Composable
fun TransactionCard(tx: CreditTransaction) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (tx.type) {
                        TransactionType.EARNED -> "Time Credits Earned"
                        TransactionType.SPENT -> "Time Credits Spent"
                        TransactionType.BONUS -> "Welcome Bonus Credits"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(Date(tx.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Balance after: ${tx.balanceAfter} credits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = when (tx.type) {
                    TransactionType.EARNED, TransactionType.BONUS -> "+${tx.amount} hrs"
                    TransactionType.SPENT -> "-${tx.amount} hrs"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when (tx.type) {
                    TransactionType.EARNED, TransactionType.BONUS -> MaterialTheme.colorScheme.primary
                    TransactionType.SPENT -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}
