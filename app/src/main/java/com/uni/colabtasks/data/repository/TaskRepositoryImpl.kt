package com.uni.colabtasks.data.repository

import com.uni.colabtasks.data.local.dao.TaskDao
import com.uni.colabtasks.data.mapper.toDomain
import com.uni.colabtasks.data.mapper.toDto
import com.uni.colabtasks.data.mapper.toEntity
import com.uni.colabtasks.data.remote.FirebaseTaskDataSource
import com.uni.colabtasks.di.IoDispatcher
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.model.TaskFilter
import com.uni.colabtasks.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao,
    private val remote: FirebaseTaskDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope
) : TaskRepository {

    private val syncJobs = ConcurrentHashMap<String, Job>()

    override fun observeTasks(listId: String, filter: TaskFilter): Flow<List<Task>> {
        val source = when (filter) {
            TaskFilter.ALL -> dao.observeAll(listId)
            TaskFilter.PENDING -> dao.observePending(listId)
            TaskFilter.DONE -> dao.observeDone(listId)
        }
        return source.map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeCounts(listId: String): Flow<TaskCounts> =
        dao.observeCounts(listId).map { row ->
            TaskCounts(total = row.total, done = row.done, pending = row.total - row.done)
        }

    override fun observeAccessibleTasksWithDueDate(): Flow<List<Task>> =
        dao.observeAllAccessibleWithDueDate().map { entities -> entities.map { it.toDomain() } }

    override fun observeAllAccessibleTasks(): Flow<List<Task>> =
        dao.observeAllAccessible().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTask(id: String): Task? = withContext(ioDispatcher) {
        dao.findById(id)?.toDomain()
    }

    override suspend fun createTask(task: Task): String = withContext(ioDispatcher) {
        dao.upsert(task.toEntity())
        remote.upsert(task.toDto())
        task.id
    }

    override suspend fun updateTask(task: Task) = withContext(ioDispatcher) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        dao.upsert(updated.toEntity())
        remote.upsert(updated.toDto())
    }

    override suspend fun toggleCompletion(id: String, completed: Boolean) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        dao.setCompleted(id, completed, now)
        val task = dao.findById(id) ?: return@withContext
        remote.setCompletion(task.ownerId, id, completed, now)
    }

    override suspend fun deleteTask(id: String) = withContext(ioDispatcher) {
        val task = dao.findById(id) ?: return@withContext
        dao.deleteById(id)
        remote.delete(task.ownerId, id)
    }

    override suspend fun syncFromRemote(ownerId: String, listId: String) = withContext(ioDispatcher) {
        val key = "$ownerId|$listId"
        syncJobs.compute(key) { _, existing ->
            if (existing != null && existing.isActive) existing
            else remote.observeTasks(ownerId, listId)
                .onEach { dtos ->
                    dao.upsertAll(dtos.map { it.toEntity() })
                    dao.deleteByListExcept(listId, dtos.map { it.id })
                }
                .launchIn(externalScope)
        }
        Unit
    }
}
