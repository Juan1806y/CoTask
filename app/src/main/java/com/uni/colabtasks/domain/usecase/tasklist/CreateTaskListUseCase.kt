package com.uni.colabtasks.domain.usecase.tasklist

import com.uni.colabtasks.domain.repository.TaskListRepository
import javax.inject.Inject

class CreateTaskListUseCase @Inject constructor(
    private val repository: TaskListRepository
) {
    suspend operator fun invoke(
        ownerId: String,
        name: String,
        description: String?,
        contributors: List<String>
    ): Result<String> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
        }
        val cleanContributors = contributors
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains('@') }
            .distinct()
        return runCatching {
            repository.createList(
                ownerId = ownerId,
                name = trimmedName,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                contributors = cleanContributors
            )
        }
    }
}
