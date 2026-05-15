package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.AuthResult
import com.uni.colabtasks.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    fun getCurrentUserId(): String?
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): AuthResult
    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult
    suspend fun signOut()
}
