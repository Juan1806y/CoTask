package com.uni.colabtasks.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {

    /** null → desconocido (cargando); true/false → resolved */
    val isAuthenticated: StateFlow<Boolean?> = observeCurrentUser()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
