package com.uni.colabtasks.domain.usecase.tasklist

import com.uni.colabtasks.domain.repository.TaskListRepository
import javax.inject.Inject

/**
 * Al iniciar sesión, convierte cualquier invitación por email pendiente (hecha antes de que
 * el usuario tuviera cuenta) en una membresía real + puntero de lista compartida.
 */
class ResolvePendingInvitesUseCase @Inject constructor(
    private val taskListRepository: TaskListRepository
) {
    suspend operator fun invoke(uid: String, email: String) =
        taskListRepository.resolvePendingInvites(uid, email)
}
