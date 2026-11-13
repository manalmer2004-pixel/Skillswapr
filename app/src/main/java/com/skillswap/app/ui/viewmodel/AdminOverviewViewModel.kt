package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.*
import com.skillswap.app.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RecentActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val type: String // "SWAP" or "REVIEW"
)

data class AdminOverviewUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalUsers: Int = 0,
    val newUsersLast7Days: Int = 0,
    val suspendedUsers: Int = 0,
    val verifiedUsers: Int = 0,
    val totalSkills: Int = 0,
    val hiddenSkills: Int = 0,
    val swapCountPending: Int = 0,
    val swapCountAccepted: Int = 0,
    val swapCountDeclined: Int = 0,
    val swapCountCompleted: Int = 0,
    val swapCountCancelled: Int = 0,
    val totalCreditsCirculation: Double = 0.0,
    val totalCreditsTransferred: Double = 0.0,
    val totalReviews: Int = 0,
    val averageRating: Double = 0.0,
    val skillsByCategory: List<Pair<String, Int>> = emptyList(),
    val signupsLast7Days: List<Pair<String, Int>> = emptyList(),
    val swapsByStatus: List<Pair<String, Int>> = emptyList(),
    val recentActivity: List<RecentActivityItem> = emptyList()
)

class AdminOverviewViewModel(
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminOverviewUiState())
    val uiState: StateFlow<AdminOverviewUiState> = _uiState.asStateFlow()

    init {
        loadOverviewData()
    }

    fun loadOverviewData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val users = adminRepository.getAllUsers()
                val skills = adminRepository.getAllSkills()
                val swapRequests = adminRepository.getAllSwapRequests()
                val transactions = adminRepository.getAllTransactions()
                val reviews = adminRepository.getAllReviews()

                val now = System.currentTimeMillis()
                val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)

                val newUsersCount = users.count { it.createdAt > 0L && it.createdAt >= sevenDaysAgo }
                val suspendedCount = users.count { it.isSuspended }
                val verifiedCount = users.count { it.isVerified }

                val hiddenSkillsCount = skills.count { it.isHidden }

                val pendingCount = swapRequests.count { it.status == SwapRequestStatus.PENDING }
                val acceptedCount = swapRequests.count { it.status == SwapRequestStatus.ACCEPTED }
                val declinedCount = swapRequests.count { it.status == SwapRequestStatus.DECLINED }
                val completedCount = swapRequests.count { it.status == SwapRequestStatus.COMPLETED }
                val cancelledCount = swapRequests.count { it.status == SwapRequestStatus.CANCELLED }

                val creditsCirculation = users.sumOf { it.timeCreditBalance }
                val creditsTransferred = transactions.filter { it.type == TransactionType.SPENT }.sumOf { it.amount }

                val reviewsCount = reviews.size
                val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0

                // Skills per Category
                val categoryMap = skills.groupBy { it.category }.mapValues { it.value.size }
                val skillsCatList = categoryMap.toList().sortedByDescending { it.second }.take(6)

                // Signups per Day for Last 7 Days
                val dayFormat = SimpleDateFormat("EEE", Locale.US)
                val signupsList = mutableListOf<Pair<String, Int>>()
                for (i in 6 downTo 0) {
                    val dayTime = now - (i * 24 * 60 * 60 * 1000L)
                    val dayStart = dayTime - (dayTime % (24 * 60 * 60 * 1000L))
                    val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
                    val label = dayFormat.format(Date(dayTime))
                    val count = users.count { it.createdAt in dayStart..<dayEnd }
                    signupsList.add(Pair(label, count))
                }

                // Swaps by Status
                val swapsStatusList = listOf(
                    Pair("Pending", pendingCount),
                    Pair("Accepted", acceptedCount),
                    Pair("Completed", completedCount),
                    Pair("Declined", declinedCount),
                    Pair("Cancelled", cancelledCount)
                )

                // Recent Activity
                val recentSwaps = swapRequests.map { req ->
                    RecentActivityItem(
                        id = req.id,
                        title = "Swap Request: ${req.skillTitle}",
                        subtitle = "Status: ${req.status.name} · Provider: ${req.providerName}",
                        timestamp = req.createdAt,
                        type = "SWAP"
                    )
                }

                val recentReviews = reviews.map { rev ->
                    RecentActivityItem(
                        id = rev.id,
                        title = "Review: ${rev.rating} ★",
                        subtitle = rev.comment.ifBlank { "No comment left" },
                        timestamp = rev.createdAt,
                        type = "REVIEW"
                    )
                }

                val activityList = (recentSwaps + recentReviews)
                    .sortedByDescending { it.timestamp }
                    .take(8)

                _uiState.value = AdminOverviewUiState(
                    isLoading = false,
                    totalUsers = users.size,
                    newUsersLast7Days = newUsersCount,
                    suspendedUsers = suspendedCount,
                    verifiedUsers = verifiedCount,
                    totalSkills = skills.size,
                    hiddenSkills = hiddenSkillsCount,
                    swapCountPending = pendingCount,
                    swapCountAccepted = acceptedCount,
                    swapCountDeclined = declinedCount,
                    swapCountCompleted = completedCount,
                    swapCountCancelled = cancelledCount,
                    totalCreditsCirculation = creditsCirculation,
                    totalCreditsTransferred = creditsTransferred,
                    totalReviews = reviewsCount,
                    averageRating = avgRating,
                    skillsByCategory = skillsCatList,
                    signupsLast7Days = signupsList,
                    swapsByStatus = swapsStatusList,
                    recentActivity = activityList
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load overview data"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
