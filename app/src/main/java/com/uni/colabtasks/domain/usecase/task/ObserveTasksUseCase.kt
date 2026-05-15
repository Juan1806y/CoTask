package com.uni.colabtasks.domain.usecase.task

import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskFilter
import com.uni.colabtasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(listId: String, filter: TaskFilter): Flow<List<Task>> =
        repository.observeTasks(listId, filter)
}
