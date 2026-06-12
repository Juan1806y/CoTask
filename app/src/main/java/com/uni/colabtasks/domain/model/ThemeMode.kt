package com.uni.colabtasks.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val onboardingDone: Boolean = false,
    val biometricEnabled: Boolean = false
)
