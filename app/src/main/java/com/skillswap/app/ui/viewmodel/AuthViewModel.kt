package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.UserProfile
import com.skillswap.app.data.repository.AuthRepository
import com.skillswap.app.data.repository.AuthResult
import com.skillswap.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userProfile: UserProfile? = null,
    val isAuthenticated: Boolean = false,
    val isAdmin: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isAuthenticated = authRepository.isUserLoggedIn))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.isUserLoggedIn) {
            loadUserProfile()
        }
    }

    fun signUp(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signUp(name, email, pass)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = result.data,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signIn(email, pass)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = result.data,
                        isAuthenticated = true,
                        isAdmin = result.data.isAdmin,
                        errorMessage = null
                    )
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun signInAdmin(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in admin email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signInAdmin(email, pass)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = result.data,
                        isAuthenticated = true,
                        isAdmin = true,
                        errorMessage = null
                    )
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun loadUserProfile() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val profile = userRepository.getUserProfile(uid)
            _uiState.value = _uiState.value.copy(
                userProfile = profile,
                isAdmin = profile?.isAdmin ?: false
            )
        }
    }

    fun updateBio(newBio: String) {
        val currentProfile = _uiState.value.userProfile ?: UserProfile(bio = newBio)
        val updatedProfile = currentProfile.copy(bio = newBio)
        _uiState.value = _uiState.value.copy(userProfile = updatedProfile)

        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.updateUserProfile(updatedProfile.copy(id = uid))
        }
    }

    fun signOut(onLoggedOut: () -> Unit) {
        authRepository.signOut()
        _uiState.value = AuthUiState(isAuthenticated = false)
        onLoggedOut()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
