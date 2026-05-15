package com.uni.colabtasks.domain.usecase.task

import com.uni.colabtasks.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteTask(id)
}
