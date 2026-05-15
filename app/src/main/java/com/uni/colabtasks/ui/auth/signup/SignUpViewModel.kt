package com.uni.colabtasks.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.AuthResult
import com.uni.colabtasks.domain.usecase.auth.SignUpWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signedUp: Boolean = false
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(displayName = value, errorMessage = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    fun signUp() {
        val s = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = signUpWithEmailUseCase(s.email, s.password, s.displayName)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, signedUp = true)
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
