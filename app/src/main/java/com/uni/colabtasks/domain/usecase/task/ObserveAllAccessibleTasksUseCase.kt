package com.uni.colabtasks.domain.usecase.task

import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Todas las tareas accesibles (propias + compartidas) — base del panel de estadísticas. */
class ObserveAllAccessibleTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> = repository.observeAllAccessibleTasks()
}
