package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.RealtimeFeeds
import com.apptesters.data.dto.NotificationDto
import com.apptesters.data.repository.InboxRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ROUTE_REGEX = Regex("\"route\"\\s*:\\s*\"app/([^/\"]+)")

private fun routeAppId(data: String?): String? = ROUTE_REGEX.find(data ?: "")?.groupValues?.get(1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onOpenApp: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val r = InboxRepository().listNotifications()
        r.onSuccess { list = it; error = null }
        r.onFailure { if (list.isEmpty()) error = it.userMessage() }
        loading = false
    }

    LaunchedEffect(Unit) {
        refresh()
        while (true) {
            delay(60_000) // safety net; realtime below does the live updates
            refresh()
        }
    }

    LaunchedEffect(Unit) {
        runCatching { RealtimeFeeds.inboxChanges().collect { refresh() } }
    }

    val unread = list.count { it.readAt == null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(
                        onClick = { scope.launch { InboxRepository().markRead(); refresh() } },
                        enabled = unread > 0
                    ) { Text("Mark all read") }
                }
            )
        }
    ) { innerPadding ->
        when {
            loading -> LoadingBox(Modifier.padding(innerPadding))
            error != null -> ErrorBox(error!!, onRetry = { loading = true; scope.launch { refresh() } }, modifier = Modifier.padding(innerPadding))
            list.isEmpty() -> EmptyBox("No notifications yet.", Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { n ->
                    val isUnread = n.readAt == null
                    val appId = routeAppId(n.data)
                    Card(
                        onClick = {
                            scope.launch {
                                if (isUnread) { InboxRepository().markRead(listOf(n.id)); refresh() }
                                if (appId != null) onOpenApp(appId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnread) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isUnread) {
                                    Text("● ", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Text(
                                    n.title.ifBlank { n.event.replace('_', ' ').replaceFirstChar { it.uppercase() } },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            if (n.body.isNotBlank()) Text(n.body, fontSize = 14.sp)
                            Text(shortTime(n.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
