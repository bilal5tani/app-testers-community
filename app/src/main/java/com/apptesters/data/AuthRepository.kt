package com.apptesters.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID

class AuthRepository(private val context: Context) {
    private val supabase = SupabaseClientProvider.client

    val currentSession
        get() = supabase.auth.currentSessionOrNull()

    val currentUser
        get() = supabase.auth.currentUserOrNull()

    val isSignedOut: Boolean
        get() = currentSession == null

    suspend fun signInWithGoogle(webClientId: String): Result<Unit> = runCatching {
        val rawNonce = UUID.randomUUID().toString()
        val digest = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

            supabase.auth.signInWith(IDToken) {
                idToken = googleIdTokenCredential.idToken
                provider = Google
                nonce = rawNonce
            }
        } catch (e: GetCredentialException) {
            // Filter Logcat by "ATC_AUTH" to see the real reason.
            Log.e("ATC_AUTH", "type=${e.type} class=${e.javaClass.simpleName} msg=${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e("ATC_AUTH", "sign-in failed: ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    suspend fun registerDeviceToken(fcmToken: String) = runCatching {
        supabase.postgrest.rpc("rpc_register_device", buildJsonObject {
            put("p_fcm_token", fcmToken)
            put("p_platform", "android")
        })
    }
}