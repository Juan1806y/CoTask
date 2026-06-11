package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.model.TaskFilter
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(listId: String, filter: TaskFilter): Flow<List<Task>>
    fun observeCounts(listId: String): Flow<TaskCounts>
    fun observeAccessibleTasksWithDueDate(): Flow<List<Task>>
    fun observeAllAccessibleTasks(): Flow<List<Task>>
    suspend fun getTask(id: String): Task?
    suspend fun createTask(task: Task): String
    suspend fun updateTask(task: Task)
    suspend fun toggleCompletion(id: String, completed: Boolean)
    suspend fun deleteTask(id: String)
    /**
     * Mantiene sincronizadas las tareas de un dueño (un solo listener por `ownerId`).
     * Reconcilia atómicamente contra las listas conocidas localmente.
     */
    suspend fun syncTasksForOwner(ownerId: String)
}
