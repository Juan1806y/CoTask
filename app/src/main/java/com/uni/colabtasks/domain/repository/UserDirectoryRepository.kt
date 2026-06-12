package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.MemberRole
import com.uni.colabtasks.domain.model.UserProfile

/** Invitación pendiente: un email fue invitado a una lista antes de tener cuenta. */
data class PendingInvite(
    val listId: String,
    val ownerId: String,
    val role: MemberRole
)

interface UserDirectoryRepository {
    suspend fun saveProfile(profile: UserProfile)
    suspend fun resolveUidByEmail(email: String): String?
    suspend fun getProfile(uid: String): UserProfile?

    suspend fun addPendingInvite(email: String, listId: String, ownerId: String, role: MemberRole)
    suspend fun removePendingInvite(email: String, listId: String)
    suspend fun fetchPendingInvites(email: String): List<PendingInvite>
}
