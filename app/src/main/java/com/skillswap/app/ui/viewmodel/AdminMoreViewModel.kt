package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminMoreUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminMoreViewModel(
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminMoreUiState())
    val uiState: StateFlow<AdminMoreUiState> = _uiState.asStateFlow()

    fun broadcastAnnouncement(adminId: String, title: String, message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = adminRepository.broadcastAnnouncement(adminId, title, message)
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Announcement broadcast sent to $count users!"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Broadcast failed"
                )
            }
        }
    }

    suspend fun generateUsersCsv(): String {
        val users = adminRepository.getAllUsers()
        val sb = StringBuilder()
        sb.append("ID,Name,Email,Handle,Location,TimeCreditBalance,IsVerified,IsSuspended,IsAdmin,JoinedDate\n")
        for (u in users) {
            val nameClean = u.name.replace(",", " ")
            val locClean = u.location.replace(",", " ")
            sb.append("${u.id},$nameClean,${u.email},${u.handle},$locClean,${u.timeCreditBalance},${u.isVerified},${u.isSuspended},${u.isAdmin},${u.joinedDate}\n")
        }
        return sb.toString()
    }

    suspend fun generateTransactionsCsv(): String {
        val txs = adminRepository.getAllTransactions()
        val sb = StringBuilder()
        sb.append("ID,UserId,SessionId,Type,Amount,BalanceAfter,CreatedAt,Reason,AdminId\n")
        for (tx in txs) {
            val reasonClean = tx.reason.replace(",", " ")
            sb.append("${tx.id},${tx.userId},${tx.sessionId},${tx.type.name},${tx.amount},${tx.balanceAfter},${tx.createdAt},$reasonClean,${tx.adminId}\n")
        }
        return sb.toString()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
