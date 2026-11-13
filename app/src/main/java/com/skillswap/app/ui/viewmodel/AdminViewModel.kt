package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.skillswap.app.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdminUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
