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
}
