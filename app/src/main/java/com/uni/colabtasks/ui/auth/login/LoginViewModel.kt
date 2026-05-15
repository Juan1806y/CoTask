package com.uni.colabtasks.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.AuthResult
import com.uni.colabtasks.domain.usecase.auth.SignInWithEmailUseCase
import com.uni.colabtasks.domain.usecase.auth.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    fun signIn() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            handle(signInWithEmailUseCase(state.email, state.password))
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            handle(signInWithGoogleUseCase(idToken))
        }
    }

    fun reportGoogleFailure(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private fun handle(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> _uiState.update {
                it.copy(isLoading = false, signedIn = true)
            }
            is AuthResult.Error -> _uiState.update {
                it.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }
}
