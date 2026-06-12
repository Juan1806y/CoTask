package com.uni.colabtasks.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uni.colabtasks.domain.usecase.auth.ObserveCurrentUserUseCase
import com.uni.colabtasks.domain.usecase.tasklist.ResolvePendingInvitesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val resolvePendingInvites: ResolvePendingInvitesUseCase
) : ViewModel() {

    private var resolvedForUid: String? = null

    /** null → desconocido (cargando); true/false → resolved */
    val isAuthenticated: StateFlow<Boolean?> = observeCurrentUser()
        .map { user ->
            // Al autenticarse, resuelve invitaciones pendientes una sola vez por uid.
            if (user != null && !user.email.isNullOrBlank() && resolvedForUid != user.id) {
                resolvedForUid = user.id
                viewModelScope.launch {
                    runCatching { resolvePendingInvites(user.id, user.email) }
                }
            }
            user != null
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
