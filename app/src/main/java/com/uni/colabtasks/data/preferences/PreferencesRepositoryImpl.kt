package com.uni.colabtasks.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.uni.colabtasks.domain.model.AppPreferences
import com.uni.colabtasks.domain.model.ThemeMode
import com.uni.colabtasks.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.preferencesDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val DynamicColor = booleanPreferencesKey("dynamic_color")
    }

    override val preferences: Flow<AppPreferences> = context.preferencesDataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[Keys.ThemeMode]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DynamicColor] ?: false
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.preferencesDataStore.edit { it[Keys.ThemeMode] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.preferencesDataStore.edit { it[Keys.DynamicColor] = enabled }
    }
}
