package com.uni.colabtasks.data.repository

import com.uni.colabtasks.data.local.dao.TaskListDao
import com.uni.colabtasks.data.mapper.toDomain
import com.uni.colabtasks.data.mapper.toDto
import com.uni.colabtasks.data.mapper.toEntity
import com.uni.colabtasks.data.remote.FirebaseTaskDataSource
import com.uni.colabtasks.data.remote.FirebaseTaskListDataSource
import com.uni.colabtasks.data.remote.dto.TaskListDto
import com.uni.colabtasks.di.IoDispatcher
import com.uni.colabtasks.domain.model.TaskList
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.repository.UserDirectoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskListRepositoryImpl @Inject constructor(
    private val dao: TaskListDao,
    private val remote: FirebaseTaskListDataSource,
    private val remoteTasks: FirebaseTaskDataSource,
    private val userDirectory: UserDirectoryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope
) : TaskListRepository {

    private val ownedSyncJobs = ConcurrentHashMap<String, Job>()
    private val sharedSyncJobs = ConcurrentHashMap<String, Job>()

    // ----- Owned + Shared list observation -----

    /**
     * Devuelve listas accesibles para `currentUid`:
     * - Owned: filas de Room con `ownerId == currentUid`
     * - Shared: traídas en vivo de RTDB via `sharedListPointers` y observadas en cada
     *   tree del owner correspondiente. No se persisten en Room (online-only para shared).
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeLists(ownerId: String): Flow<List<TaskList>> {
        val ownedFlow: Flow<List<TaskList>> = dao.observeByOwner(ownerId)
            .onStart { startOwnedSync(ownerId) }
            .map { entities -> entities.map { it.toDomain() } }

        val sharedFlow: Flow<List<TaskList>> = remote.observeSharedPointers(ownerId)
            .flatMapLatest { pointers ->
                if (pointers.isEmpty()) flowOf(emptyList())
                else combineLatestPerPointer(pointers.map { it.ownerId to it.listId })
            }

        return combine(ownedFlow, sharedFlow) { owned, shared ->
            // Las shared aparecen después; orden: favoritos primero (ya viene de Room),
            // luego shared. Evita duplicados (en caso raro de que un owner se invite a sí mismo).
            val seenIds = owned.map { it.id }.toHashSet()
            owned + shared.filter { it.id !in seenIds }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun combineLatestPerPointer(pointers: List<Pair<String, String>>): Flow<List<TaskList>> {
        // pointers: (ownerId, listId). Cacheamos el DTO en Room en cada emisión para
        // que `getList`/`observeList` funcionen también para listas compartidas.
        val flows: List<Flow<TaskList?>> = pointers.map { (owner, listId) ->
            remote.observeList(owner, listId)
                .onEach { dto -> dto?.let { dao.upsert(it.toEntity()) } }
                .map { dto -> dto?.toDomainTaskList() }
        }
        return combine(flows) { array -> array.toList().filterNotNull() }
    }

    private fun TaskListDto.toDomainTaskList(): TaskList = TaskList(
        id = id,
        ownerId = ownerId,
        name = name,
        description = description,
        isFavorite = isFavorite,
        contributors = contributors,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    override fun observeFavorites(ownerId: String): Flow<List<TaskList>> {
        return dao.observeFavoritesByOwner(ownerId)
            .onStart { startOwnedSync(ownerId) }
            .map { entities -> entities.map { it.toDomain() } }
    }

    private fun startOwnedSync(ownerId: String) {
        // compute() en vez de computeIfAbsent() para sustituir Jobs muertos
        // (p.ej. tras un permission-denied que cerró el listener).
        ownedSyncJobs.compute(ownerId) { _, existing ->
            if (existing != null && existing.isActive) existing
            else remote.observeLists(ownerId)
                .onEach { dtos ->
                    dao.upsertAll(dtos.map { it.toEntity() })
                    dao.deleteByOwnerExcept(ownerId, dtos.map { it.id })
                }
                .launchIn(externalScope)
        }
    }

    override fun observeList(id: String): Flow<TaskList?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getList(id: String): TaskList? = withContext(ioDispatcher) {
        dao.findById(id)?.toDomain()
    }

    // ----- CRUD with contributor resolution -----

    override suspend fun createList(
        ownerId: String,
        name: String,
        description: String?,
        contributors: List<String>
    ): String = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val list = TaskList(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            name = name,
            description = description,
            isFavorite = false,
            contributors = contributors,
            createdAt = now,
            updatedAt = now
        )
        val memberIds = resolveContributors(ownerId, contributors)
        dao.upsert(list.toEntity())
        remote.upsert(list.toDto(memberIds = memberIds))
        // Plant pointers for each new member (skip the owner if accidentally included).
        memberIds.filter { it != ownerId }.forEach { memberUid ->
            runCatching { remote.addSharedPointer(memberUid, list.id, ownerId) }
        }
        list.id
    }

    override suspend fun updateList(
        id: String,
        name: String,
        description: String?,
        contributors: List<String>
    ) = withContext(ioDispatcher) {
        val current = dao.findById(id)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()
        val updated = current.copy(
            name = name,
            description = description,
            contributors = contributors,
            updatedAt = now
        )
        // Get previous memberIds from remote to compute the diff for pointers.
        val previousDto = runCatching { remote.fetchList(current.ownerId, id) }.getOrNull()
        val previousMemberIds = previousDto?.memberIds.orEmpty().toSet()
        val newMemberIds = resolveContributors(current.ownerId, contributors).toSet()

        dao.upsert(updated.toEntity())
        remote.upsert(updated.toDto(memberIds = newMemberIds.toList()))

        // Pointer diff:
        val added = newMemberIds - previousMemberIds
        val removed = previousMemberIds - newMemberIds
        added.filter { it != current.ownerId }.forEach { memberUid ->
            runCatching { remote.addSharedPointer(memberUid, id, current.ownerId) }
        }
        removed.forEach { memberUid ->
            runCatching { remote.removeSharedPointer(memberUid, id) }
        }
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        dao.setFavorite(id, favorite, now)
        val current = dao.findById(id)?.toDomain() ?: return@withContext
        // Preserve existing memberIds in RTDB by fetching the current DTO first.
        val previousDto = runCatching { remote.fetchList(current.ownerId, id) }.getOrNull()
        remote.upsert(
            current.copy(isFavorite = favorite, updatedAt = now)
                .toDto(memberIds = previousDto?.memberIds.orEmpty())
        )
    }

    override suspend fun deleteList(id: String) = withContext(ioDispatcher) {
        val current = dao.findById(id) ?: return@withContext
        // Clean up shared pointers for all members before deleting.
        val previousDto = runCatching { remote.fetchList(current.ownerId, id) }.getOrNull()
        previousDto?.memberIds.orEmpty().forEach { memberUid ->
            runCatching { remote.removeSharedPointer(memberUid, id) }
        }
        dao.deleteById(id)
        remoteTasks.deleteForList(current.ownerId, id)
        remote.delete(current.ownerId, id)
    }

    override suspend fun syncFromRemote(ownerId: String) = withContext(ioDispatcher) {
        val remoteLists = remote.fetchLists(ownerId)
        dao.upsertAll(remoteLists.map { it.toEntity() })
        dao.deleteByOwnerExcept(ownerId, remoteLists.map { it.id })
    }

    /**
     * Convierte una lista de emails de contribuyentes en uids vía el directorio.
     * Los emails no resueltos se descartan silenciosamente (el usuario no existe).
     */
    private suspend fun resolveContributors(ownerId: String, emails: List<String>): List<String> {
        val resolved = mutableListOf<String>()
        for (email in emails) {
            val uid = runCatching { userDirectory.resolveUidByEmail(email) }.getOrNull()
            if (!uid.isNullOrEmpty() && uid != ownerId) resolved += uid
        }
        return resolved.distinct()
    }
}
