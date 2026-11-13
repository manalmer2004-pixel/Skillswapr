package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.CreditTransaction
import com.skillswap.app.data.model.SwapRequest
import com.skillswap.app.data.model.SwapRequestStatus
import com.skillswap.app.data.model.UserProfile
import com.skillswap.app.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnomalyFlag(
    val title: String,
    val description: String,
    val severity: String // "HIGH" or "MEDIUM"
)

data class AdminActivityUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTab: Int = 0, // 0 = Swap Requests, 1 = Credit Ledger
    val swapStatusFilter: String = "All", // "All", "PENDING", "ACCEPTED", "COMPLETED", "CANCELLED", "DECLINED"
    val txTypeFilter: String = "All", // "All", "EARNED", "SPENT", "BONUS", "ADMIN_ADJUSTMENT"
    val searchQuery: String = "",
    val swapRequests: List<SwapRequest> = emptyList(),
    val transactions: List<CreditTransaction> = emptyList(),
    val users: List<UserProfile> = emptyList(),
    val anomalies: List<AnomalyFlag> = emptyList()
)

class AdminActivityViewModel(
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminActivityUiState())
    val uiState: StateFlow<AdminActivityUiState> = _uiState.asStateFlow()

    init {
        loadActivity()
    }

    fun loadActivity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val swaps = adminRepository.getAllSwapRequests().sortedByDescending { it.createdAt }
                val txs = adminRepository.getAllTransactions().sortedByDescending { it.createdAt }
                val users = adminRepository.getAllUsers()

                // Compute Anomalies
                val anomalyList = mutableListOf<AnomalyFlag>()

                // 1. Negative Balance Users
                val negativeUsers = users.filter { it.timeCreditBalance < 0.0 }
                for (u in negativeUsers) {
                    anomalyList.add(
                        AnomalyFlag(
                            title = "Negative Credit Balance",
                            description = "User ${u.name} (${u.email}) has negative balance: ${u.timeCreditBalance} hrs",
                            severity = "HIGH"
                        )
                    )
                }

                // 2. High Cancelled Swaps Rate (3+ cancelled)
                val cancelledGroup = swaps.filter { it.status == SwapRequestStatus.CANCELLED }
                    .groupBy { it.requesterId }

                for ((requesterId, reqs) in cancelledGroup) {
                    if (reqs.size >= 3) {
                        val name = reqs.firstOrNull()?.providerName ?: requesterId
                        anomalyList.add(
                            AnomalyFlag(
                                title = "High Swap Cancellation Rate",
                                description = "User ID '$requesterId' has ${reqs.size} cancelled swap requests.",
                                severity = "MEDIUM"
                            )
                        )
                    }
                }

                // 3. Large Transaction Amounts (>= 10 hrs)
                val largeTxs = txs.filter { it.amount >= 10.0 }
                for (tx in largeTxs) {
                    anomalyList.add(
                        AnomalyFlag(
                            title = "Abnormally Large Transaction",
                            description = "Transaction '${tx.id}' for user '${tx.userId}' was ${tx.amount} hrs (${tx.type.name}).",
                            severity = "MEDIUM"
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    swapRequests = swaps,
                    transactions = txs,
                    users = users,
                    anomalies = anomalyList
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load activity"
                )
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun updateSwapStatusFilter(filter: String) {
        _uiState.value = _uiState.value.copy(swapStatusFilter = filter)
    }

    fun updateTxTypeFilter(filter: String) {
        _uiState.value = _uiState.value.copy(txTypeFilter = filter)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun forceCancelSwapRequest(requestId: String, adminId: String, reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.forceCancelSwapRequest(requestId, adminId, reason)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    successMessage = "Swap request force-cancelled successfully"
                )
                loadActivity()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    errorMessage = e.localizedMessage ?: "Force cancel failed"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
