package com.uni.colabtasks.domain.usecase.task

import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTasksWithDueDateUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> = repository.observeAccessibleTasksWithDueDate()
}
