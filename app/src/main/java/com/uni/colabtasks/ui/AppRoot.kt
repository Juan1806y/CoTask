package com.uni.colabtasks.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.AppPreferences
import com.uni.colabtasks.domain.repository.PreferencesRepository
import com.uni.colabtasks.ui.navigation.AppNavGraph
import com.uni.colabtasks.ui.theme.CoTaskTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel raíz para suscribir el tema a las preferencias del usuario.
 * Mantiene el Theme aplicado en toda la app sin necesidad de reinicio.
 */
@HiltViewModel
class AppRootViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())
}

@Composable
fun AppRoot(viewModel: AppRootViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    CoTaskTheme(themeMode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
        AppNavGraph()
    }
}
