package com.uni.colabtasks.domain.usecase.task

import com.uni.colabtasks.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskCompletionUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(id: String, completed: Boolean) =
        repository.toggleCompletion(id, completed)
}
