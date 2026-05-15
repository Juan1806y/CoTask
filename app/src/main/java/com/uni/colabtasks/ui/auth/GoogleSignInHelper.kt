package com.uni.colabtasks.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Encapsula la obtención del idToken de Google con Credential Manager.
 * El idToken obtenido se entrega al [com.uni.colabtasks.data.remote.FirebaseAuthDataSource]
 * para canjearlo por una sesión Firebase.
 */
class GoogleSignInHelper(
    private val context: Context,
    private val webClientId: String
) {
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    sealed interface Result {
        data class Success(val idToken: String) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun requestIdToken(): Result {
        if (webClientId.isBlank() || webClientId == "REPLACE_WITH_YOUR_WEB_CLIENT_ID") {
            return Result.Failure(
                "Configura default_web_client_id en strings.xml con el Web Client ID de Firebase."
            )
        }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.Success(googleCredential.idToken)
            } else {
                Result.Failure("Credencial no reconocida")
            }
        } catch (e: GetCredentialException) {
            Result.Failure(e.localizedMessage ?: "No se pudo obtener la credencial")
        } catch (e: GoogleIdTokenParsingException) {
            Result.Failure(e.localizedMessage ?: "Token de Google inválido")
        }
    }
}
