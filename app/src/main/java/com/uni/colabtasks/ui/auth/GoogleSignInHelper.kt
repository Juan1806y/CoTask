package com.uni.colabtasks.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

private const val TAG = "GoogleSignIn"

/**
 * Encapsula la obtención del idToken de Google con Credential Manager.
 *
 * Estrategia en dos pasos:
 *   1. Primero intenta `GetGoogleIdOption` filtrando solo cuentas previamente autorizadas
 *      (re-auth silencioso para usuarios recurrentes).
 *   2. Si no hay ninguna cuenta autorizada (NoCredentialException), reintenta con
 *      `GetSignInWithGoogleOption` que **siempre** muestra el selector explícito de
 *      cuentas de Google — esto es lo que necesitas la primera vez.
 *
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
        data object Cancelled : Result
    }

    suspend fun requestIdToken(): Result {
        if (webClientId.isBlank() || webClientId == "REPLACE_WITH_YOUR_WEB_CLIENT_ID") {
            return Result.Failure(
                "Configura default_web_client_id en strings.xml con el Web Client ID de Firebase."
            )
        }

        // Paso 1: intento silencioso con cuentas previamente autorizadas.
        val silentResult = tryGetCredential(
            request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(true)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(true)
                        .build()
                )
                .build()
        )
        if (silentResult is Result.Success) return silentResult
        if (silentResult is Result.Cancelled) return silentResult

        // Paso 2: fallback con picker explícito de Sign-In With Google.
        return tryGetCredential(
            request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(webClientId).build()
                )
                .build()
        )
    }

    private suspend fun tryGetCredential(request: GetCredentialRequest): Result = try {
        val response = credentialManager.getCredential(context, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Result.Success(googleCredential.idToken)
        } else {
            Result.Failure("Credencial no reconocida (${credential.type})")
        }
    } catch (e: NoCredentialException) {
        Log.w(TAG, "NoCredentialException: ${e.message}")
        Result.Failure(
            "No se encontraron credenciales de Google. Verifica que: " +
                "1) Hay una cuenta Google en el dispositivo (Configuración → Cuentas). " +
                "2) Tu SHA-1 de debug está registrado en Firebase Console. " +
                "3) El Web Client ID en strings.xml corresponde al OAuth de tu proyecto."
        )
    } catch (e: GetCredentialCancellationException) {
        // "Cancelled" puede significar dos cosas muy distintas:
        // 1) El usuario cerró el picker manualmente (back / tap fuera)
        // 2) El picker se auto-cerró por mismatch de SHA-1 / Web Client ID / package
        //    — Google Play Services a veces reporta esto como cancelled en vez de
        //    como error explícito, confusamente.
        // Loggeamos el detail para poder diferenciar al inspeccionar logcat.
        Log.i(TAG, "Sign-in cancelled. type=${e.type} message='${e.message}' errorMessage='${e.errorMessage}'")
        Result.Cancelled
    } catch (e: GetCredentialException) {
        Log.w(TAG, "GetCredentialException: type=${e.type} message='${e.message}' errorMessage='${e.errorMessage}'")
        Result.Failure(e.localizedMessage ?: "No se pudo obtener la credencial")
    } catch (e: GoogleIdTokenParsingException) {
        Log.e(TAG, "Failed to parse Google ID token", e)
        Result.Failure(e.localizedMessage ?: "Token de Google inválido")
    }
}
