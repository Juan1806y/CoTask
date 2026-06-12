package com.uni.colabtasks.data.repository

import android.util.Log
import com.uni.colabtasks.data.local.dao.TaskListDao
import com.uni.colabtasks.data.mapper.toDomain
import com.uni.colabtasks.data.mapper.toDto
import com.uni.colabtasks.data.mapper.toEntity
import com.uni.colabtasks.data.remote.FirebaseTaskDataSource
import com.uni.colabtasks.data.remote.FirebaseTaskListDataSource
import com.uni.colabtasks.data.remote.dto.InvitationDto
import com.uni.colabtasks.data.remote.dto.TaskListDto
import com.uni.colabtasks.di.IoDispatcher
import com.uni.colabtasks.domain.model.Invitation
import com.uni.colabtasks.domain.model.ListMember
import com.uni.colabtasks.domain.model.MemberRole
import com.uni.colabtasks.domain.model.TaskList
import com.uni.colabtasks.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository,
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
                .map { dto -> dto?.toDomain() }
        }
        return combine(flows) { array -> array.toList().filterNotNull() }
    }

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
        editorEmails: List<String>,
        viewerEmails: List<String>
    ): String = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        // La lista nace solo con el owner: los miembros se agregan al ACEPTAR la invitación.
        val list = TaskList(
            id = id,
            ownerId = ownerId,
            name = name,
            description = description,
            isFavorite = false,
            contributors = editorEmails,
            viewerEmails = viewerEmails,
            memberIds = emptyList(),
            viewerIds = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(list.toEntity())
        remote.upsert(list.toDto())
        inviteEmails(ownerId, id, name, editorEmails, MemberRole.EDITOR)
        inviteEmails(ownerId, id, name, viewerEmails, MemberRole.VIEWER)
        id
    }

    override suspend fun updateList(
        id: String,
        name: String,
        description: String?,
        editorEmails: List<String>,
        viewerEmails: List<String>
    ) = withContext(ioDispatcher) {
        val current = dao.findById(id)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()

        // Mantenemos memberIds/viewerIds tal cual (se gestionan por aceptación/revocación).
        val updated = current.copy(
            name = name,
            description = description,
            contributors = editorEmails,
            viewerEmails = viewerEmails,
            updatedAt = now
        )
        dao.upsert(updated.toEntity())
        remote.upsert(updated.toDto())

        // Emails nuevos → invitación. Emails quitados → revocar acceso si ya era miembro.
        val previousEmails = (current.contributors + current.viewerEmails).map { it.lowercase() }.toSet()
        val newEditors = editorEmails.filter { it.lowercase() !in previousEmails }
        val newViewers = viewerEmails.filter { it.lowercase() !in previousEmails }
        inviteEmails(current.ownerId, id, name, newEditors, MemberRole.EDITOR)
        inviteEmails(current.ownerId, id, name, newViewers, MemberRole.VIEWER)

        val currentEmails = (editorEmails + viewerEmails).map { it.lowercase() }.toSet()
        val removedEmails = previousEmails - currentEmails
        removedEmails.forEach { email -> revokeEmail(current.ownerId, id, email) }
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        dao.setFavorite(id, favorite, now)
        val current = dao.findById(id)?.toDomain() ?: return@withContext
        remote.upsert(current.copy(isFavorite = favorite, updatedAt = now).toDto())
    }

    override suspend fun getListMembers(list: TaskList): List<ListMember> = withContext(ioDispatcher) {
        val members = mutableListOf<ListMember>()
        // Owner
        val ownerProfile = runCatching { userDirectory.getProfile(list.ownerId) }.getOrNull()
        members += ListMember(
            uid = list.ownerId,
            email = ownerProfile?.email,
            displayName = ownerProfile?.displayName,
            role = MemberRole.OWNER
        )
        // Editors
        list.memberIds.forEach { uid ->
            val p = runCatching { userDirectory.getProfile(uid) }.getOrNull()
            members += ListMember(uid, p?.email, p?.displayName, MemberRole.EDITOR)
        }
        // Viewers
        list.viewerIds.forEach { uid ->
            val p = runCatching { userDirectory.getProfile(uid) }.getOrNull()
            members += ListMember(uid, p?.email, p?.displayName, MemberRole.VIEWER)
        }
        members
    }

    override suspend fun resolvePendingInvites(uid: String, email: String) = withContext(ioDispatcher) {
        // Las invitaciones por email (hechas antes de tener cuenta) se convierten en
        // invitaciones reales que el usuario podrá aceptar o rechazar.
        val invites = runCatching { userDirectory.fetchPendingInvites(email) }.getOrDefault(emptyList())
        invites.forEach { invite ->
            runCatching {
                val listDto = remote.fetchList(invite.ownerId, invite.listId)
                if (listDto != null) {
                    val inviterName = userDirectory.getProfile(invite.ownerId)?.displayName ?: "Alguien"
                    remote.createInvitation(
                        inviteeUid = uid,
                        dto = InvitationDto(
                            listId = invite.listId,
                            ownerId = invite.ownerId,
                            listName = listDto.name,
                            inviterName = inviterName,
                            role = invite.role.name,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                userDirectory.removePendingInvite(email, invite.listId)
            }.onFailure { Log.w(TAG, "resolve pending invite ${invite.listId} failed: ${it.message}") }
        }
    }

    // ----- Invitaciones aceptar/rechazar -----

    override fun observeInvitations(uid: String): Flow<List<Invitation>> =
        remote.observeInvitations(uid).map { dtos ->
            dtos.map { dto ->
                Invitation(
                    listId = dto.listId,
                    ownerId = dto.ownerId,
                    listName = dto.listName,
                    inviterName = dto.inviterName,
                    role = runCatching { MemberRole.valueOf(dto.role) }.getOrDefault(MemberRole.EDITOR),
                    timestamp = dto.timestamp
                )
            }
        }

    override suspend fun acceptInvitation(uid: String, invitation: Invitation) = withContext(ioDispatcher) {
        // Aceptar = pasar a ser miembro (en el árbol del dueño) + ver la lista + quitar invitación.
        runCatching {
            remote.addMemberUid(
                ownerId = invitation.ownerId,
                listId = invitation.listId,
                uid = uid,
                asViewer = invitation.role == MemberRole.VIEWER
            )
            remote.addSharedPointer(uid, invitation.listId, invitation.ownerId)
        }.onFailure { Log.w(TAG, "acceptInvitation ${invitation.listId} failed: ${it.message}") }
        runCatching { remote.deleteInvitation(uid, invitation.listId) }
        Unit
    }

    override suspend fun rejectInvitation(uid: String, invitation: Invitation) = withContext(ioDispatcher) {
        runCatching { remote.deleteInvitation(uid, invitation.listId) }
        Unit
    }

    override suspend fun deleteList(id: String) = withContext(ioDispatcher) {
        val current = dao.findById(id)?.toDomain() ?: return@withContext
        // Room es la fuente de verdad: borramos local primero (esto siempre debe ocurrir).
        dao.deleteById(id)
        // El resto es best-effort hacia remoto; nunca debe tumbar la app si falla.
        (current.memberIds + current.viewerIds).forEach { memberUid ->
            runCatching { remote.removeSharedPointer(memberUid, id) }
        }
        remoteTasks.deleteForList(current.ownerId, id)
        runCatching { remote.delete(current.ownerId, id) }
            .onFailure { Log.w(TAG, "remote list delete failed for $id: ${it.message}") }
    }

    override suspend fun restoreList(list: TaskList) = withContext(ioDispatcher) {
        dao.upsert(list.toEntity())
        remote.upsert(list.toDto())
        (list.memberIds + list.viewerIds).filter { it != list.ownerId }.forEach { memberUid ->
            runCatching { remote.addSharedPointer(memberUid, list.id, list.ownerId) }
        }
    }

    override suspend fun syncFromRemote(ownerId: String) = withContext(ioDispatcher) {
        val remoteLists = remote.fetchLists(ownerId)
        dao.upsertAll(remoteLists.map { it.toEntity() })
        dao.deleteByOwnerExcept(ownerId, remoteLists.map { it.id })
    }

    /**
     * Por cada email: si tiene cuenta → crea una invitación que podrá aceptar/rechazar;
     * si aún no tiene cuenta → guarda una invitación pendiente (se materializa al registrarse).
     */
    private suspend fun inviteEmails(
        ownerId: String,
        listId: String,
        listName: String,
        emails: List<String>,
        role: MemberRole
    ) {
        val inviterName = authRepository.getCurrentDisplayName() ?: "Alguien"
        for (email in emails) {
            val uid = runCatching { userDirectory.resolveUidByEmail(email) }.getOrNull()
            if (!uid.isNullOrEmpty() && uid != ownerId) {
                runCatching {
                    remote.createInvitation(
                        inviteeUid = uid,
                        dto = InvitationDto(
                            listId = listId,
                            ownerId = ownerId,
                            listName = listName,
                            inviterName = inviterName,
                            role = role.name,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } else if (uid.isNullOrEmpty()) {
                runCatching { userDirectory.addPendingInvite(email, listId, ownerId, role) }
            }
        }
    }

    /** Revoca el acceso de un email quitado de la lista (miembro, puntero e invitación). */
    private suspend fun revokeEmail(ownerId: String, listId: String, email: String) {
        val uid = runCatching { userDirectory.resolveUidByEmail(email) }.getOrNull()
        if (!uid.isNullOrEmpty()) {
            runCatching { remote.removeMemberUid(ownerId, listId, uid) }
            runCatching { remote.removeSharedPointer(uid, listId) }
            runCatching { remote.deleteInvitation(uid, listId) }
        }
        runCatching { userDirectory.removePendingInvite(email, listId) }
    }

    private companion object {
        const val TAG = "TaskListRepository"
    }
}
