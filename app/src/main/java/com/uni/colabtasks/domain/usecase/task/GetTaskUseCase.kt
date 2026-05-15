package com.uni.colabtasks.domain.usecase.task

import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(id: String): Task? = repository.getTask(id)
}
