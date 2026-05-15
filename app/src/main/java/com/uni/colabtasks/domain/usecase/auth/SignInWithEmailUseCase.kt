package com.uni.colabtasks.domain.usecase.auth

import com.uni.colabtasks.domain.model.AuthResult
import com.uni.colabtasks.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Error("Email y contraseña son obligatorios")
        }
        return authRepository.signInWithEmail(email.trim(), password)
    }
}
