package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.UserAvailability
import com.skillswap.app.data.repository.AvailabilityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AvailabilityViewModel(
    private val repository: AvailabilityRepository = AvailabilityRepository()
) : ViewModel() {

    private val _availabilities = MutableStateFlow<List<UserAvailability>>(emptyList())
    val availabilities: StateFlow<List<UserAvailability>> = _availabilities.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    fun loadAvailability(userId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getAvailabilityFlow(userId)
                .catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { _availabilities.value = it }
        }
    }

    fun saveAvailability(availability: UserAvailability) {
        viewModelScope.launch {
            repository.saveAvailability(availability)
        }
    }

    fun deleteAvailability(availabilityId: String) {
        viewModelScope.launch {
            repository.deleteAvailability(availabilityId)
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
