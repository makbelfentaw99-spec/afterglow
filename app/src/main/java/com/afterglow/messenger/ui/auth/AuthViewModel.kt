package com.afterglow.messenger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglow.messenger.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class NeedsUsername(val uid: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

// No constructor parameters on purpose: Compose's viewModel() needs a
// reflectively-discoverable no-arg constructor, which a Kotlin default
// parameter does NOT reliably provide at the bytecode level. Instantiating
// the repository as a plain property sidesteps that.
class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(username: String, displayName: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords don't match.")
            return
        }
        if (username.isBlank()) {
            _uiState.value = AuthUiState.Error("Please choose a username.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.register(username.trim(), displayName.trim(), password)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Registration failed. Please try again.") }
            )
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Enter your username and password.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.login(username.trim(), password)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Sign in failed. Please try again.") }
            )
        }
    }

    fun onGoogleIdToken(idToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogleIdToken(idToken)
            _uiState.value = result.fold(
                onSuccess = { outcome ->
                    if (outcome.needsUsername) {
                        AuthUiState.NeedsUsername(outcome.uid)
                    } else {
                        AuthUiState.Success
                    }
                },
                onFailure = { AuthUiState.Error(it.message ?: "Google sign-in failed. Please try again.") }
            )
        }
    }

    // A user dismissing the Google account picker isn't an error worth
    // surfacing — just go back to a neutral state.
    fun onGoogleSignInCancelled() {
        _uiState.value = AuthUiState.Idle
    }

    fun onGoogleSignInFailed(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun claimUsername(uid: String, username: String, displayName: String) {
        if (username.isBlank()) {
            _uiState.value = AuthUiState.Error("Please choose a username.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.claimUsername(uid, username.trim(), displayName.trim())
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Couldn't save that username. Please try again.") }
            )
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
