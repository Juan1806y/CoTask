package com.uni.colabtasks.domain.model

data class Task(
    val id: String,
    val listId: String,
    val ownerId: String,
    val title: String,
    val description: String?,
    val category: String?,
    val isCompleted: Boolean,
    val dueDate: Long?,
    val priority: Priority = Priority.NONE,
    val assignedTo: String? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
) {
    val subtaskDoneCount: Int get() = subtasks.count { it.isDone }
}
