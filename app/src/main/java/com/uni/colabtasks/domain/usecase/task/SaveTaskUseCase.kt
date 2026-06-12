package com.uni.colabtasks.domain.usecase.task

import com.uni.colabtasks.domain.model.Priority
import com.uni.colabtasks.domain.model.Recurrence
import com.uni.colabtasks.domain.model.Subtask
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.repository.TaskRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Crea o actualiza una tarea según si existe un id previo.
 * Devuelve el id (nuevo si era creación).
 */
class SaveTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(
        existingId: String?,
        listId: String,
        ownerId: String,
        title: String,
        description: String?,
        category: String?,
        dueDate: Long?,
        priority: Priority = Priority.NONE,
        assignedTo: String? = null,
        recurrence: Recurrence = Recurrence.NONE,
        subtasks: List<Subtask> = emptyList()
    ): Result<String> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("El título es obligatorio"))
        }
        val cleanSubtasks = subtasks.filter { it.title.isNotBlank() }
        val now = System.currentTimeMillis()
        return runCatching {
            if (existingId == null) {
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    ownerId = ownerId,
                    title = trimmedTitle,
                    description = description?.trim()?.takeIf { it.isNotEmpty() },
                    category = category?.trim()?.takeIf { it.isNotEmpty() },
                    isCompleted = false,
                    dueDate = dueDate,
                    priority = priority,
                    assignedTo = assignedTo,
                    recurrence = recurrence,
                    subtasks = cleanSubtasks,
                    createdAt = now,
                    updatedAt = now
                )
                repository.createTask(task)
            } else {
                val current = repository.getTask(existingId)
                    ?: throw IllegalStateException("La tarea ya no existe")
                val updated = current.copy(
                    title = trimmedTitle,
                    description = description?.trim()?.takeIf { it.isNotEmpty() },
                    category = category?.trim()?.takeIf { it.isNotEmpty() },
                    dueDate = dueDate,
                    priority = priority,
                    assignedTo = assignedTo,
                    recurrence = recurrence,
                    subtasks = cleanSubtasks,
                    updatedAt = now
                )
                repository.updateTask(updated)
                existingId
            }
        }
    }
}
