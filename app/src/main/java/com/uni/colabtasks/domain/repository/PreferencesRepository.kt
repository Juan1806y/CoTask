package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.AppPreferences
import com.uni.colabtasks.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<AppPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setOnboardingDone(done: Boolean)
    suspend fun setBiometricEnabled(enabled: Boolean)
}
