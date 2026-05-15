package com.uni.colabtasks.domain.usecase.tasklist

import com.uni.colabtasks.domain.repository.TaskListRepository
import javax.inject.Inject

class UpdateTaskListUseCase @Inject constructor(
    private val repository: TaskListRepository
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        description: String?,
        contributors: List<String>
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
        }
        val cleanContributors = contributors
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains('@') }
            .distinct()
        return runCatching {
            repository.updateList(
                id = id,
                name = trimmedName,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                contributors = cleanContributors
            )
        }
    }
}
