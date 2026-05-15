package com.uni.colabtasks.domain.usecase.task

import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTaskCountsUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(listId: String): Flow<TaskCounts> = repository.observeCounts(listId)
}
