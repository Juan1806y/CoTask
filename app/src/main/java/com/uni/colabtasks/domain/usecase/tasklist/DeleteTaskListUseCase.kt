package com.uni.colabtasks.domain.usecase.tasklist

import com.uni.colabtasks.domain.repository.TaskListRepository
import javax.inject.Inject

class DeleteTaskListUseCase @Inject constructor(
    private val repository: TaskListRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteList(id)
}
