package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.UserFavorite
import com.skillswap.app.data.repository.FavoriteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class FavoriteViewModel(
    private val repository: FavoriteRepository = FavoriteRepository()
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<UserFavorite>>(emptyList())
    val favorites: StateFlow<List<UserFavorite>> = _favorites.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    fun loadFavorites(userId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getFavoritesFlow(userId)
                .catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { _favorites.value = it }
        }
    }

    fun toggleFavorite(userId: String, skillId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(userId, skillId)
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
