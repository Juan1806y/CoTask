package com.uni.colabtasks.data.repository

import com.uni.colabtasks.data.remote.FirebaseUserDirectoryDataSource
import com.uni.colabtasks.data.remote.dto.UserProfileDto
import com.uni.colabtasks.domain.model.UserProfile
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
}
