package com.uni.colabtasks.data.remote

import com.google.firebase.database.DatabaseReference
import com.uni.colabtasks.data.remote.dto.UserProfileDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maneja el directorio de usuarios en RTDB:
 *   /userProfiles/{uid}            ← datos del usuario
 *   /usersByEmail/{emailKey}: uid  ← índice por email para invitar contribuyentes
 *
 * `emailKey` es el email normalizado a una key segura para RTDB (sin '.', '/', '$', '[', ']', '#').
 */
@Singleton
class FirebaseUserDirectoryDataSource @Inject constructor(
    private val rootRef: DatabaseReference
) {

    suspend fun saveProfile(dto: UserProfileDto) {
        rootRef.child("userProfiles").child(dto.uid).setValue(dto).await()
        val emailKey = emailToKey(dto.email)
        if (emailKey.isNotEmpty()) {
            rootRef.child("usersByEmail").child(emailKey).setValue(dto.uid).await()
        }
    }

    suspend fun findUidByEmail(email: String): String? {
        val emailKey = emailToKey(email)
        if (emailKey.isEmpty()) return null
        val snapshot = rootRef.child("usersByEmail").child(emailKey).get().await()
        return snapshot.getValue(String::class.java)
    }

    suspend fun findProfile(uid: String): UserProfileDto? {
        val snapshot = rootRef.child("userProfiles").child(uid).get().await()
        return snapshot.getValue(UserProfileDto::class.java)
    }

    companion object {
        /**
         * RTDB keys no admiten estos caracteres: '.', '$', '#', '[', ']', '/'.
         * Reemplazamos cada uno con guion bajo y normalizamos a lowercase.
         */
        fun emailToKey(email: String): String {
            val normalized = email.trim().lowercase()
            if (normalized.isEmpty()) return ""
            return normalized
                .replace('.', '_')
                .replace('$', '_')
                .replace('#', '_')
                .replace('[', '_')
                .replace(']', '_')
                .replace('/', '_')
        }
    }
}
