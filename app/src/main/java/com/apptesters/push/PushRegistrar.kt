package com.apptesters.push

import android.content.Context
import android.util.Log
import com.apptesters.data.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PushRegistrar {
    /** Fetches the current FCM token and stores it with rpc_register_device. Never throws. */
    suspend fun registerCurrentToken(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.e("ATC_PUSH", "Firebase not initialised: google-services plugin / google-services.json missing")
                return
            }
            val token = fetchToken() ?: return
            AuthRepository(context.applicationContext).registerDeviceToken(token)
        } catch (e: Exception) {
            Log.e("ATC_PUSH", "register failed: ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    private suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
        }
    }
}