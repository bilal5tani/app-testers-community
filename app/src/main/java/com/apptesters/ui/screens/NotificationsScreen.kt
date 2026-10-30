package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.RealtimeFeeds
import com.apptesters.data.dto.NotificationDto
import com.apptesters.data.repository.InboxRepository
import com.apptesters.domain.userMessage
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import com.apptesters.ui.theme.Lime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId

private val ROUTE_REGEX = Regex("\"route\"\\s*:\\s*\"app/([^/\"]+)")
private fun routeAppId(data: String?): String? = ROUTE_REGEX.find(data ?: "")?.groupValues?.get(1)

private fun parse(iso: String): OffsetDateTime? = runCatching { OffsetDateTime.parse(iso) }.getOrNull()

private fun ago(iso: String): String {
    val t = parse(iso) ?: return shortTime(iso)
    val m = Duration.between(t, OffsetDateTime.now()).toMinutes().coerceAtLeast(0)
    return when {
        m < 1 -> "Just now"
        m < 60 -> "$m min ago"
        m < 60 * 24 -> "${m / 60} hours ago"
        m < 60 * 48 -> "Yesterday"
        else -> "${m / (60 * 24)} days ago"
    }
}

private fun isToday(iso: String): Boolean {
    val t = parse(iso) ?: return false
    val z = ZoneId.systemDefault()
    return t.atZoneSameInstant(z).toLocalDate() == OffsetDateTime.now().atZoneSameInstant(z).toLocalDate()
}

private fun iconFor(event: String) = when {
    "join" in event -> AtcIcons.Plus to Lime
    "check" in event || "complet" in event || "milestone" in event || "qualif" in event -> AtcIcons.Check to Color(0xFFDCD6FF)
    "nudge" in event || "miss" in event || "remov" in event || "drop" in event -> AtcIcons.Warn to Color(0xFFFFD9B8)
    else -> AtcIcons.Arrow to Color(0xFFFFE9A8)
}

@Composable
fun NotificationsScreen(onOpenApp: (String) -> Unit = {}, onBack: (() -> Unit)? = null) {
    val c = Atc.colors
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
        while (true) { delay(60_000); refresh() }
    }
    LaunchedEffect(Unit) { runCatching { RealtimeFeeds.inboxChanges().collect { refresh() } } }

    val unread = list.count { it.readAt == null }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = if (onBack != null) 12.dp else 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) { AtcIcon(AtcIcons.Back, c.ink, 24.dp) }
                Spacer(Modifier.width(4.dp))
            }
            Text("Notifications", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Box(Modifier.clip(CircleShape).clickable(enabled = unread > 0) { scope.launch { InboxRepository().markRead(); refresh() } }.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text("Mark all read", color = if (unread > 0) c.ink else c.muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { loading = true; scope.launch { refresh() } })
            list.isEmpty() -> EmptyBox("No notifications yet.")
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                val (today, earlier) = list.partition { isToday(it.createdAt) }
                listOf("Today" to today, "Earlier" to earlier).filter { it.second.isNotEmpty() }.forEach { (title, items) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(title.uppercase(), color = c.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.8.sp)
                        AtcCard {
                            items.forEachIndexed { i, n ->
                                if (i > 0) HorizontalDivider(color = c.divider)
                                val isUnread = n.readAt == null
                                val appId = routeAppId(n.data)
                                val (icon, bg) = iconFor(n.event)
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        scope.launch {
                                            if (isUnread) { InboxRepository().markRead(listOf(n.id)); refresh() }
                                            if (appId != null) onOpenApp(appId)
                                        }
                                    }.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                                        AtcIcon(icon, Color(0xFF121212), 20.dp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            n.title.ifBlank { n.event.replace('_', ' ').replaceFirstChar { it.uppercase() } },
                                            color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp
                                        )
                                        if (n.body.isNotBlank()) Text(n.body, color = c.ink, fontSize = 14.sp, lineHeight = 19.sp)
                                        Text(ago(n.createdAt), color = c.muted, fontSize = 13.sp)
                                    }
                                    if (isUnread) {
                                        Box(Modifier.padding(top = 6.dp, start = 8.dp).size(10.dp).clip(CircleShape).background(if (c.isDark) c.accent else c.ink))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
