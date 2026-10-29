package com.apptesters.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Realtime "something changed" signals. Each emission means: reload the screen's data.
 * Row-level security is enforced by Supabase, so you only get events you may read.
 */
object RealtimeFeeds {
    private val client get() = SupabaseClientProvider.client

    /** Owner's Live Progress: check-ins and testers of one app. */
    fun progressChanges(appId: String): Flow<Unit> = channelFlow {
        val ch = client.channel("progress:$appId")
        val checkIns = ch.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "check_ins"
            filter("app_id", FilterOperator.EQ, appId)
        }
        val testers = ch.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "app_testers"
            filter("app_id", FilterOperator.EQ, appId)
        }
        ch.subscribe()
        launch { checkIns.collect { send(Unit) } }
        launch { testers.collect { send(Unit) } }
        awaitClose { CoroutineScope(Dispatchers.IO).launch { runCatching { client.realtime.removeChannel(ch) } } }
    }

    /** Inbox: new notification rows for the signed-in user. */
    fun inboxChanges(): Flow<Unit> = channelFlow {
        val userId = client.auth.currentUserOrNull()?.id ?: return@channelFlow
        val ch = client.channel("inbox:$userId")
        val inserts = ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "notifications"
            filter("user_id", FilterOperator.EQ, userId)
        }
        ch.subscribe()
        launch { inserts.collect { send(Unit) } }
        awaitClose { CoroutineScope(Dispatchers.IO).launch { runCatching { client.realtime.removeChannel(ch) } } }
    }
}
