package com.uni.colabtasks.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.model.AppPreferences
import com.uni.colabtasks.domain.repository.PreferencesRepository
import com.uni.colabtasks.ui.lock.LockScreen
import com.uni.colabtasks.ui.lock.canUseBiometrics
import com.uni.colabtasks.ui.navigation.AppNavGraph
import com.uni.colabtasks.ui.onboarding.OnboardingScreen
import com.uni.colabtasks.ui.theme.CoTaskTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel raíz: aplica el tema y decide si mostrar onboarding o la app.
 */
@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    fun completeOnboarding() {
        viewModelScope.launch { preferencesRepository.setOnboardingDone(true) }
    }
}

@Composable
fun AppRoot(viewModel: AppRootViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Se mantiene desbloqueado durante la sesión; vuelve a pedirse en arranque en frío.
    var unlocked by remember { mutableStateOf(false) }
    val lockActive = prefs.biometricEnabled && canUseBiometrics(context) && !unlocked

    CoTaskTheme(themeMode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
        when {
            !prefs.onboardingDone -> OnboardingScreen(onFinish = viewModel::completeOnboarding)
            lockActive -> LockScreen(onUnlocked = { unlocked = true })
            else -> AppNavGraph()
        }
    }
}
