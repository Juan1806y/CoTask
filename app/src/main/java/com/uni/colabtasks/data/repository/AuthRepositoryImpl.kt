package com.uni.colabtasks.data.repository

import com.uni.colabtasks.data.remote.FirebaseAuthDataSource
import com.uni.colabtasks.domain.model.AuthResult
import com.uni.colabtasks.domain.model.User
import com.uni.colabtasks.domain.model.UserProfile
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.repository.UserDirectoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val userDirectoryRepository: UserDirectoryRepository
) : AuthRepository {

    override val currentUser: Flow<User?> = authDataSource.observeUser()

    override fun getCurrentUserId(): String? = authDataSource.currentUserId()

    override fun getCurrentDisplayName(): String? = authDataSource.currentDisplayName()

    override suspend fun signInWithEmail(email: String, password: String): AuthResult =
        runAndRegister { authDataSource.signInEmail(email, password) }
            ?: AuthResult.Error("No se pudo iniciar sesión")

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): AuthResult = runAndRegister { authDataSource.signUpEmail(email, password, displayName) }
        ?: AuthResult.Error("No se pudo crear la cuenta")

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult =
        runAndRegister { authDataSource.signInGoogle(idToken) }
            ?: AuthResult.Error("No se pudo iniciar con Google")

    override suspend fun updateDisplayName(displayName: String): AuthResult {
        val name = displayName.trim()
        if (name.isEmpty()) return AuthResult.Error("El nombre no puede estar vacío")
        return try {
            val user = authDataSource.updateDisplayName(name)
            // Reflejar el nuevo nombre en el directorio para invitaciones por email.
            user.email?.let { email ->
                runCatching {
                    userDirectoryRepository.saveProfile(
                        UserProfile(
                            uid = user.id,
                            email = email,
                            displayName = user.displayName,
                            photoUrl = user.photoUrl
                        )
                    )
                }
            }
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "No se pudo actualizar el perfil")
        }
    }

    override suspend fun signOut() {
        authDataSource.signOut()
    }

    /**
     * Ejecuta la operación de auth y, si se obtiene un usuario válido, registra (idempotente)
     * el perfil en el directorio remoto para que pueda ser invitado por email por otros.
     */
    private suspend inline fun runAndRegister(block: () -> User): AuthResult? = try {
        val user = block()
        val email = user.email
        if (!email.isNullOrBlank()) {
            // No bloqueante para el éxito del login si esto falla
            runCatching {
                userDirectoryRepository.saveProfile(
                    UserProfile(
                        uid = user.id,
                        email = email,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl
                    )
                )
            }
        }
        AuthResult.Success(user)
    } catch (e: Exception) {
        AuthResult.Error(e.localizedMessage ?: "Error de autenticación")
    }
}
