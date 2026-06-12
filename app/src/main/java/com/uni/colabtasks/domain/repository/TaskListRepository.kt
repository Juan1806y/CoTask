package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.Invitation
import com.uni.colabtasks.domain.model.ListMember
import com.uni.colabtasks.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface TaskListRepository {
    fun observeLists(ownerId: String): Flow<List<TaskList>>
    fun observeFavorites(ownerId: String): Flow<List<TaskList>>
    fun observeList(id: String): Flow<TaskList?>
    suspend fun getList(id: String): TaskList?
    /** Resuelve los miembros (owner + editores + viewers) a perfiles para mostrar/asignar. */
    suspend fun getListMembers(list: TaskList): List<ListMember>
    /** Al iniciar sesión: convierte invitaciones por email pendientes en invitaciones reales. */
    suspend fun resolvePendingInvites(uid: String, email: String)

    // ----- Invitaciones aceptar/rechazar -----
    fun observeInvitations(uid: String): Flow<List<Invitation>>
    suspend fun acceptInvitation(uid: String, invitation: Invitation)
    suspend fun rejectInvitation(uid: String, invitation: Invitation)
    suspend fun createList(
        ownerId: String,
        name: String,
        description: String?,
        editorEmails: List<String>,
        viewerEmails: List<String>
    ): String
    suspend fun updateList(
        id: String,
        name: String,
        description: String?,
        editorEmails: List<String>,
        viewerEmails: List<String>
    )
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun deleteList(id: String)
    /** Re-inserta una lista existente verbatim (mismo id) — usado para deshacer un borrado. */
    suspend fun restoreList(list: TaskList)
    suspend fun syncFromRemote(ownerId: String)
}
