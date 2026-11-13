package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.Category
import com.skillswap.app.data.model.Review
import com.skillswap.app.data.model.SkillListing
import com.skillswap.app.data.repository.AdminRepository
import com.skillswap.app.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminContentUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedSection: Int = 0, // 0 = Skills, 1 = Reviews, 2 = Categories
    val searchQuery: String = "",
    val categoryFilter: String = "All",
    val hiddenFilter: String = "All", // "All", "Visible", "Hidden"
    val skills: List<SkillListing> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val categories: List<Category> = emptyList()
)

class AdminContentViewModel(
    private val adminRepository: AdminRepository = AdminRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminContentUiState())
    val uiState: StateFlow<AdminContentUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val skills = adminRepository.getAllSkills()
                val reviews = adminRepository.getAllReviews().sortedByDescending { it.createdAt }
                categoryRepository.getCategoriesFlow().collect { cats ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        skills = skills,
                        reviews = reviews,
                        categories = cats
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to load content"
                )
            }
        }
    }

    fun selectSection(section: Int) {
        _uiState.value = _uiState.value.copy(selectedSection = section)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateCategoryFilter(cat: String) {
        _uiState.value = _uiState.value.copy(categoryFilter = cat)
    }

    fun updateHiddenFilter(filter: String) {
        _uiState.value = _uiState.value.copy(hiddenFilter = filter)
    }

    fun toggleSkillHidden(skillId: String, currentHidden: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.updateSkillHidden(skillId, !currentHidden)
            result.onSuccess {
                val msg = if (!currentHidden) "Skill hidden from community feed" else "Skill is now visible"
                _uiState.value = _uiState.value.copy(isActionLoading = false, successMessage = msg)
                refreshSkills()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = e.localizedMessage ?: "Action failed")
            }
        }
    }

    fun deleteSkill(skillId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.deleteSkill(skillId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isActionLoading = false, successMessage = "Skill deleted successfully")
                refreshSkills()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = e.localizedMessage ?: "Delete failed")
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.deleteReview(reviewId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isActionLoading = false, successMessage = "Review removed successfully")
                refreshReviews()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = e.localizedMessage ?: "Delete failed")
            }
        }
    }

    fun addCategory(name: String, order: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.addCategory(name, order)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isActionLoading = false, successMessage = "Category added successfully")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = e.localizedMessage ?: "Add failed")
            }
        }
    }

    fun updateCategory(categoryId: String, newName: String, newOrder: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.updateCategory(categoryId, newName, newOrder)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isActionLoading = false, successMessage = "Category updated successfully")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = e.localizedMessage ?: "Update failed")
            }
        }
    }

    fun deleteCategory(categoryId: String, categoryName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            val result = adminRepository.deleteCategory(categoryId, categoryName)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isActionLoading = false, successMessage = "Category deleted successfully")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isActionLoading = false, errorMessage = e.localizedMessage ?: "Delete failed")
            }
        }
    }

    private fun refreshSkills() {
        viewModelScope.launch {
            val updatedSkills = adminRepository.getAllSkills()
            _uiState.value = _uiState.value.copy(skills = updatedSkills)
        }
    }

    private fun refreshReviews() {
        viewModelScope.launch {
            val updatedReviews = adminRepository.getAllReviews().sortedByDescending { it.createdAt }
            _uiState.value = _uiState.value.copy(reviews = updatedReviews)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
