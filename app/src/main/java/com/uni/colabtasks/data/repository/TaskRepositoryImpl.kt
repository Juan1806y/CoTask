package com.uni.colabtasks.data.repository

import android.util.Log
import com.uni.colabtasks.data.local.dao.TaskDao
import com.uni.colabtasks.data.mapper.toDomain
import com.uni.colabtasks.data.mapper.toDto
import com.uni.colabtasks.data.mapper.toEntity
import com.uni.colabtasks.data.remote.FirebaseTaskDataSource
import com.uni.colabtasks.di.IoDispatcher
import com.uni.colabtasks.domain.model.ActivityAction
import com.uni.colabtasks.domain.model.Recurrence
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskCounts
import com.uni.colabtasks.domain.model.TaskFilter
import com.uni.colabtasks.domain.model.nextOccurrence
import com.uni.colabtasks.domain.repository.ActivityRepository
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.repository.TaskRepository
import com.uni.colabtasks.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao,
    private val remote: FirebaseTaskDataSource,
    private val reminderScheduler: ReminderScheduler,
    private val activityRepository: ActivityRepository,
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope
) : TaskRepository {

    private val syncJobs = ConcurrentHashMap<String, Job>()

    /** Registra actividad de forma best-effort (nunca debe romper la operación principal). */
    private suspend fun logActivity(task: Task, action: ActivityAction) {
        val actorUid = authRepository.getCurrentUserId() ?: return
        val actorName = authRepository.getCurrentDisplayName() ?: "Alguien"
        runCatching {
            activityRepository.log(
                listOwnerId = task.ownerId,
                listId = task.listId,
                actorUid = actorUid,
                actorName = actorName,
                action = action,
                targetTitle = task.title
            )
        }
    }

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
        reminderScheduler.schedule(task)
        logActivity(task, ActivityAction.CREATED)
        task.id
    }

    override suspend fun updateTask(task: Task) = withContext(ioDispatcher) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        dao.upsert(updated.toEntity())
        remote.upsert(updated.toDto())
        reminderScheduler.schedule(updated)
        logActivity(updated, ActivityAction.EDITED)
    }

    override suspend fun toggleCompletion(id: String, completed: Boolean) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        dao.setCompleted(id, completed, now)
        val task = dao.findById(id) ?: return@withContext
        remote.setCompletion(task.ownerId, id, completed, now)
        // Completar cancela el recordatorio; reabrir lo reprograma.
        reminderScheduler.schedule(task.toDomain())
        logActivity(task.toDomain(), if (completed) ActivityAction.COMPLETED else ActivityAction.REOPENED)

        // Tareas recurrentes: al completarse, genera la siguiente ocurrencia.
        if (completed) {
            val domain = task.toDomain()
            val nextDue = nextOccurrence(domain.dueDate, domain.recurrence)
            if (domain.recurrence != Recurrence.NONE && nextDue != null) {
                val next = domain.copy(
                    id = UUID.randomUUID().toString(),
                    isCompleted = false,
                    dueDate = nextDue,
                    subtasks = domain.subtasks.map { it.copy(isDone = false) },
                    createdAt = now,
                    updatedAt = now
                )
                createTask(next)
            }
        }
    }

    override suspend fun deleteTask(id: String) = withContext(ioDispatcher) {
        val task = dao.findById(id) ?: return@withContext
        dao.deleteById(id)
        remote.delete(task.ownerId, id)
        reminderScheduler.cancel(id)
        logActivity(task.toDomain(), ActivityAction.DELETED)
    }

    override suspend fun syncTasksForOwner(ownerId: String) = withContext(ioDispatcher) {
        // Un solo listener por dueño. Dedup por ownerId (no por lista) — así borrar una lista
        // no afecta el sync de las demás del mismo dueño.
        syncJobs.compute(ownerId) { _, existing ->
            if (existing != null && existing.isActive) existing
            else remote.observeOwnerTasks(ownerId)
                .onEach { dtos ->
                    runCatching {
                        // Insertamos solo tareas cuya lista existe localmente (respeta la FK
                        // y evita traer tareas de listas a las que no tenemos acceso).
                        val known = dao.knownListIds().toHashSet()
                        val visible = dtos.filter { it.listId in known }
                        dao.upsertAll(visible.map { it.toEntity() })
                        // Para la reconciliación de borrado usamos TODOS los ids remotos como
                        // keep-set (no el filtrado). Así solo borramos tareas que realmente
                        // desaparecieron en remoto, nunca tareas válidas cuya lista aún no se
                        // ha cacheado (evita un wipe por carrera lista-vs-tarea en el arranque).
                        dao.deleteForOwnerExcept(ownerId, dtos.map { it.id })
                    }.onFailure { e ->
                        Log.w(TAG, "owner task sync skipped for $ownerId: ${e.message}")
                    }
                }
                .launchIn(externalScope)
        }
        Unit
    }

    private companion object {
        const val TAG = "TaskRepository"
    }
}
