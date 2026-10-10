package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.UserAvailability
import com.skillswap.app.data.repository.AvailabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AvailabilityViewModel(
    private val repository: AvailabilityRepository = AvailabilityRepository()
) : ViewModel() {

    private val _availabilities = MutableStateFlow<List<UserAvailability>>(emptyList())
    val availabilities: StateFlow<List<UserAvailability>> = _availabilities.asStateFlow()

    fun loadAvailability(userId: String) {
        viewModelScope.launch {
            repository.getAvailabilityFlow(userId).collect {
                _availabilities.value = it
            }
        }
    }

    fun saveAvailability(availability: UserAvailability) {
        viewModelScope.launch {
            repository.saveAvailability(availability)
        }
    }
}
