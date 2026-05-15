package com.uni.colabtasks.domain.usecase.auth

import com.uni.colabtasks.domain.model.User
import com.uni.colabtasks.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> = authRepository.currentUser
}
