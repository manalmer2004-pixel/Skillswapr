package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.Review
import com.skillswap.app.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val repository: ReviewRepository = ReviewRepository()
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    private val _ratingSummary = MutableStateFlow<Pair<Double, Long>>(Pair(0.0, 0L))
    val ratingSummary: StateFlow<Pair<Double, Long>> = _ratingSummary.asStateFlow()

    private val _ratingSummaries = MutableStateFlow<Map<String, Pair<Double, Long>>>(emptyMap())
    val ratingSummaries: StateFlow<Map<String, Pair<Double, Long>>> = _ratingSummaries.asStateFlow()

    fun submitReview(review: Review, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val success = repository.addReview(review)
            _isSubmitting.value = false
            if (success) {
                _resultMessage.value = "Review submitted successfully"
                onComplete()
            } else {
                _resultMessage.value = "Failed to submit review"
            }
        }
    }

    fun loadRatingSummary(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            val summary = repository.getRatingSummary(userId)
            _ratingSummary.value = summary
        }
    }

    fun loadRatingSummaries(userIds: List<String>) {
        if (userIds.isEmpty()) return
        viewModelScope.launch {
            val summaries = repository.getRatingSummaries(userIds)
            _ratingSummaries.value = summaries
        }
    }

    fun clearResultMessage() {
        _resultMessage.value = null
    }
}
