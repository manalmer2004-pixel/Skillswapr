package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.*
import com.skillswap.app.data.repository.AdminRepository
import com.skillswap.app.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUserDetailData(
    val user: UserProfile,
    val rating: Double = 0.0,
    val reviewCount: Long = 0L,
    val skills: List<SkillListing> = emptyList(),
    val swapRequests: List<SwapRequest> = emptyList(),
    val transactions: List<CreditTransaction> = emptyList()
)

data class AdminUsersUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val filterTab: String = "All", // "All", "Active", "Suspended", "Verified"
    val allUsers: List<UserProfile> = emptyList(),
    val selectedUserDetail: AdminUserDetailData? = null
)

class AdminUsersViewModel(
    private val adminRepository: AdminRepository = AdminRepository(),
    private val reviewRepository: ReviewRepository = ReviewRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val users = adminRepository.getAllUsers()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allUsers = users
                )

                // Refresh selected user details if open
                _uiState.value.selectedUserDetail?.user?.id?.let { selectedUid ->
                    val updatedUser = users.find { it.id == selectedUid }
                    if (updatedUser != null) {
                        selectUser(updatedUser)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load users"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateFilterTab(tab: String) {
        _uiState.value = _uiState.value.copy(filterTab = tab)
    }

    fun selectUser(user: UserProfile) {
        viewModelScope.launch {
            try {
                val ratingPair = reviewRepository.getRatingSummary(user.id)
                val allSkills = adminRepository.getAllSkills().filter { it.userId == user.id }
                val allSwaps = adminRepository.getAllSwapRequests().filter { it.requesterId == user.id || it.providerId == user.id }
                val userTxs = adminRepository.getUserTransactions(user.id)

                _uiState.value = _uiState.value.copy(
                    selectedUserDetail = AdminUserDetailData(
                        user = user,
                        rating = ratingPair.first,
                        reviewCount = ratingPair.second,
                        skills = allSkills,
                        swapRequests = allSwaps,
                        transactions = userTxs
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    selectedUserDetail = AdminUserDetailData(user = user)
                )
            }
        }
    }

    fun clearSelectedUser() {
        _uiState.value = _uiState.value.copy(selectedUserDetail = null)
    }

    fun suspendUser(targetUserId: String, adminId: String, suspend: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.updateUserSuspension(targetUserId, adminId, suspend)
            result.onSuccess {
                val msg = if (suspend) "User suspended successfully" else "User reactivated successfully"
                _uiState.value = _uiState.value.copy(isActionLoading = false, successMessage = msg)
                loadUsers()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = e.localizedMessage ?: "Action failed")
            }
        }
    }

    fun verifyUser(targetUserId: String, adminId: String, verify: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.updateUserVerification(targetUserId, adminId, verify)
            result.onSuccess {
                val msg = if (verify) "User verified successfully" else "User verification removed"
                _uiState.value = _uiState.value.copy(isActionLoading = false, successMessage = msg)
                loadUsers()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = e.localizedMessage ?: "Action failed")
            }
        }
    }

    fun adjustUserCredits(targetUserId: String, adminId: String, amount: Double, reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.adjustUserCredits(targetUserId, adminId, amount, reason)
            result.onSuccess { newBalance ->
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    successMessage = "Credit balance adjusted successfully. New Balance: $newBalance credits"
                )
                loadUsers()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    errorMessage = e.localizedMessage ?: "Credit adjustment failed"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
