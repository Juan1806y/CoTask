package com.uni.colabtasks.data.repository

import com.uni.colabtasks.data.remote.FirebaseUserDirectoryDataSource
import com.uni.colabtasks.data.remote.dto.UserProfileDto
import com.uni.colabtasks.domain.model.MemberRole
import com.uni.colabtasks.domain.model.UserProfile
import com.uni.colabtasks.domain.repository.PendingInvite
import com.uni.colabtasks.domain.repository.UserDirectoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDirectoryRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseUserDirectoryDataSource
) : UserDirectoryRepository {

    override suspend fun saveProfile(profile: UserProfile) {
        dataSource.saveProfile(
            UserProfileDto(
                uid = profile.uid,
                email = profile.email,
                displayName = profile.displayName,
                photoUrl = profile.photoUrl,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun resolveUidByEmail(email: String): String? =
        dataSource.findUidByEmail(email)

    override suspend fun getProfile(uid: String): UserProfile? =
        dataSource.findProfile(uid)?.let {
            UserProfile(
                uid = it.uid,
                email = it.email,
                displayName = it.displayName,
                photoUrl = it.photoUrl
            )
        }

    override suspend fun addPendingInvite(email: String, listId: String, ownerId: String, role: MemberRole) {
        dataSource.addPendingInvite(email, listId, ownerId, role.name)
    }

    override suspend fun removePendingInvite(email: String, listId: String) {
        dataSource.removePendingInvite(email, listId)
    }

    override suspend fun fetchPendingInvites(email: String): List<PendingInvite> =
        dataSource.fetchPendingInvites(email).map { (listId, ownerId, role) ->
            PendingInvite(
                listId = listId,
                ownerId = ownerId,
                role = runCatching { MemberRole.valueOf(role) }.getOrDefault(MemberRole.EDITOR)
            )
        }
}
