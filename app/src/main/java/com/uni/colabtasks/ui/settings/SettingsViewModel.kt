package com.uni.colabtasks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.AppPreferences
import com.uni.colabtasks.domain.model.AuthResult
import com.uni.colabtasks.domain.model.ThemeMode
import com.uni.colabtasks.domain.model.User
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.repository.PreferencesRepository
import com.uni.colabtasks.domain.usecase.auth.SignOutUseCase
import com.uni.colabtasks.domain.usecase.auth.UpdateDisplayNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val user: User? = null,
    val editingName: Boolean = false,
    val nameDraft: String = "",
    val savingName: Boolean = false,
    val message: String? = null,
    val signedOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val updateDisplayNameUseCase: UpdateDisplayNameUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    init {
        viewModelScope.launch {
            preferences.collect { value ->
                _uiState.update { it.copy(preferences = value) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBiometricEnabled(enabled) }
    }

    fun startEditingName() = _uiState.update {
        it.copy(editingName = true, nameDraft = it.user?.displayName.orEmpty())
    }

    fun onNameDraftChange(value: String) = _uiState.update { it.copy(nameDraft = value) }

    fun cancelEditingName() = _uiState.update { it.copy(editingName = false, nameDraft = "") }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun saveName() {
        val draft = _uiState.value.nameDraft
        _uiState.update { it.copy(savingName = true) }
        viewModelScope.launch {
            when (val result = updateDisplayNameUseCase(draft)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(savingName = false, editingName = false, nameDraft = "")
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(savingName = false, message = result.message)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _uiState.update { it.copy(signedOut = true) }
        }
    }
}
