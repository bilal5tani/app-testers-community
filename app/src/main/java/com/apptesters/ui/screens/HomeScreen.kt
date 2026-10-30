package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.dto.HomeResponseDto
import com.apptesters.data.repository.HomeRepository
import com.apptesters.data.repository.ProfileRepository
import com.apptesters.domain.userMessage
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToAppDetail: (String) -> Unit = {},
    onNavigateToLiveProgress: (String) -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToAddApp: () -> Unit = {},
) {
    val c = Atc.colors
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var data by remember { mutableStateOf<HomeResponseDto?>(null) }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkingId by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        val r = HomeRepository().getHome()
        if (r.isSuccess) { data = r.getOrNull(); error = null } else error = r.exceptionOrNull()?.userMessage()
        loading = false
    }
    LaunchedEffect(Unit) {
        load()
        ProfileRepository().getProfile().getOrNull()?.let { name = it.profile.displayName }
    }

    Box(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        val d = data
        when {
            loading -> LoadingBox()
            d == null -> ErrorBox(error ?: "Could not load.", onRetry = { scope.launch { loading = true; load() } })
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Header
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialsCircle(name.ifBlank { "?" })
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Salam,", color = c.muted, fontSize = 13.sp)
                        Text(
                            name.substringBefore(" ").ifBlank { "there" }, color = c.ink,
                            fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp
                        )
                    }
                    CircleIconButton(AtcIcons.Bell, onNavigateToInbox, badge = d.unreadNotifications > 0)
                }

                Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Hero
                    val app = d.myApp
                    if (app != null) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.heroBg).padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(app.iconUrl, app.title, 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.title, color = c.heroInk, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
                                    Text("Closed testing", color = c.heroMuted, fontSize = 13.sp)
                                }
                                Row(
                                    Modifier.border(1.5.dp, c.heroAccent, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(c.heroAccent))
                                    Spacer(Modifier.width(6.dp))
                                    Text(app.status.replaceFirstChar { it.uppercase() }, color = c.heroAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${app.joinedCount}", color = c.heroInk, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 60.sp, lineHeight = 60.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("of ${app.testersNeeded} testers", color = c.heroMuted, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                                Spacer(Modifier.weight(1f))
                                Text("Day ${app.qualifiedDays} / 14", color = c.heroAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            SegmentBar(app.qualifiedDays, 14, c.heroSegOn, c.heroSegOff)
                            PillButton(
                                "See live progress", { onNavigateToLiveProgress(app.id) },
                                Modifier.fillMaxWidth(), height = 48.dp,
                                container = if (c.isDark) c.heroInk else c.accent,
                                content = if (c.isDark) c.accent else c.onAccent,
                                trailingIcon = AtcIcons.Arrow
                            )
                        }
                    }

                    // Today's check-ins
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val done = d.todayCheckins.count { it.checkedInToday }
                        SectionHeader("Today's check-ins", "$done of ${d.todayCheckins.size} done")
                        if (d.todayCheckins.isEmpty()) {
                            AtcCard { Text("You are not testing any app yet. Find one in Discover.", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(16.dp)) }
                        } else AtcCard {
                            d.todayCheckins.forEachIndexed { i, ci ->
                                if (i > 0) HorizontalDivider(color = c.divider)
                                Row(
                                    Modifier.clickable { onNavigateToAppDetail(ci.appId) }.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(ci.iconUrl, ci.title, 44.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(ci.title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                                        Text("Streak ${ci.streak} days", color = c.muted, fontSize = 13.sp)
                                    }
                                    val isDone = ci.checkedInToday
                                    val busy = checkingId == ci.appId
                                    Box(
                                        Modifier.height(44.dp).widthIn(min = 96.dp).clip(CircleShape)
                                            .background(if (isDone) c.accent else if (c.isDark) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.ui.graphics.Color(0xFF121212))
                                            .then(if (!isDone && c.isDark) Modifier.border(1.5.dp, c.accent, CircleShape) else Modifier)
                                            .clickable(enabled = !isDone && checkingId == null) {
                                                scope.launch {
                                                    checkingId = ci.appId
                                                    val r = HomeRepository().checkIn(ci.appId)
                                                    checkingId = null
                                                    if (r.isSuccess) load() else snackbar.showSnackbar(r.exceptionOrNull()?.userMessage() ?: "Check-in failed")
                                                }
                                            }.padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (isDone) "Done today" else if (busy) "…" else "Check in",
                                            color = if (isDone) c.onAccent else c.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Needs testers
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Needs testers", "See all", onNavigateToDiscover)
                        d.needsTesters.forEach { n ->
                            AtcCard(onClick = { onNavigateToAppDetail(n.id) }) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(n.iconUrl, n.title, 48.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(n.title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                        Text("@${n.ownerName} · ${n.spotsLeft} spots left", color = c.muted, fontSize = 13.sp)
                                        ThinBar((12 - n.spotsLeft).coerceAtLeast(0) / 12f)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    AtcIcon(AtcIcons.Chevron, c.ink, 20.dp)
                                }
                            }
                        }
                        DashedButton("Add your app to the queue", AtcIcons.Plus, onNavigateToAddApp)
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
