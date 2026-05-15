package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.UserProfile

interface UserDirectoryRepository {
    suspend fun saveProfile(profile: UserProfile)
    suspend fun resolveUidByEmail(email: String): String?
    suspend fun getProfile(uid: String): UserProfile?
}
