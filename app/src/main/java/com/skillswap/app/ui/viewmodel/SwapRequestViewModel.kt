package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.SwapRequest
import com.skillswap.app.data.model.SwapRequestStatus
import com.skillswap.app.data.repository.SwapRequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SwapRequestViewModel(
    private val repository: SwapRequestRepository = SwapRequestRepository()
) : ViewModel() {

    private val _requests = MutableStateFlow<List<SwapRequest>>(emptyList())
    val requests: StateFlow<List<SwapRequest>> = _requests.asStateFlow()

    private val _receivedRequests = MutableStateFlow<List<SwapRequest>>(emptyList())
    val receivedRequests: StateFlow<List<SwapRequest>> = _receivedRequests.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    fun loadRequests(userId: String) {
        viewModelScope.launch {
            repository.getSwapRequestsFlow(userId).collect {
                _requests.value = it
            }
        }
        viewModelScope.launch {
            repository.getReceivedRequestsFlow(userId).collect {
                _receivedRequests.value = it
            }
        }
    }

    fun createRequest(request: SwapRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            val success = repository.createSwapRequest(request)
            if (success) {
                onDone()
            } else {
                _actionResult.value = "Failed to create swap request"
            }
        }
    }

    fun updateStatus(requestId: String, newStatus: SwapRequestStatus) {
        viewModelScope.launch {
            val result = repository.updateStatus(requestId, newStatus)
            if (result.isFailure) {
                _actionResult.value = result.exceptionOrNull()?.message ?: "Operation failed"
            } else {
                _actionResult.value = "Status updated to ${newStatus.name}"
            }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}
