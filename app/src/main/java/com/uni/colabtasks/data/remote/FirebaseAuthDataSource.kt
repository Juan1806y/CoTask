package com.uni.colabtasks.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.userProfileChangeRequest
import com.uni.colabtasks.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth
) {
    fun currentUserId(): String? = auth.currentUser?.uid

    fun observeUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomain())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInEmail(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.toDomain() ?: error("Firebase no devolvió un usuario")
    }

    suspend fun signUpEmail(email: String, password: String, displayName: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: error("Firebase no devolvió un usuario")
        val update: UserProfileChangeRequest = userProfileChangeRequest {
            this.displayName = displayName
        }
        firebaseUser.updateProfile(update).await()
        return firebaseUser.toDomain().copy(displayName = displayName)
    }

    suspend fun signInGoogle(idToken: String): User {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user?.toDomain() ?: error("Firebase no devolvió un usuario")
    }

    fun signOut() = auth.signOut()

    private fun com.google.firebase.auth.FirebaseUser.toDomain() = User(
        id = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString()
    )
}
