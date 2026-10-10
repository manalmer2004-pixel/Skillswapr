package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.UserFavorite
import com.skillswap.app.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoriteViewModel(
    private val repository: FavoriteRepository = FavoriteRepository()
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<UserFavorite>>(emptyList())
    val favorites: StateFlow<List<UserFavorite>> = _favorites.asStateFlow()

    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            repository.getFavoritesFlow(userId).collect {
                _favorites.value = it
            }
        }
    }

    fun toggleFavorite(userId: String, skillId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(userId, skillId)
        }
    }
}
