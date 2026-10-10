package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.Category
import com.skillswap.app.data.repository.CategoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: CategoryRepository = CategoryRepository()
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getCategoriesFlow()
                .catch { e -> _errorMessage.value = e.localizedMessage }
                .collect { _categories.value = it }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
