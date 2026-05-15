package com.uni.colabtasks.domain.usecase.auth

import com.uni.colabtasks.domain.model.AuthResult
import com.uni.colabtasks.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): AuthResult {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            return AuthResult.Error("Todos los campos son obligatorios")
        }
        if (password.length < 6) {
            return AuthResult.Error("La contraseña debe tener al menos 6 caracteres")
        }
        return authRepository.signUpWithEmail(email.trim(), password, displayName.trim())
    }
}
