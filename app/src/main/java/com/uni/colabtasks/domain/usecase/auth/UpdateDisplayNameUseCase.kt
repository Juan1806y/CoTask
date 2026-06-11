package com.uni.colabtasks.domain.usecase.auth

import com.uni.colabtasks.domain.model.AuthResult
import com.uni.colabtasks.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateDisplayNameUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(displayName: String): AuthResult =
        authRepository.updateDisplayName(displayName)
}
