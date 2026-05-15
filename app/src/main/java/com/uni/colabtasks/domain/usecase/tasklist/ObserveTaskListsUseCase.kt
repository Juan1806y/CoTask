package com.uni.colabtasks.domain.usecase.tasklist

import com.uni.colabtasks.domain.model.TaskList
import com.uni.colabtasks.domain.repository.TaskListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTaskListsUseCase @Inject constructor(
    private val repository: TaskListRepository
) {
    operator fun invoke(ownerId: String): Flow<List<TaskList>> =
        repository.observeLists(ownerId)
}
