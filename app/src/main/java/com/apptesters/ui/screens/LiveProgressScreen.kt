package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import com.apptesters.ui.theme.Lime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.RealtimeFeeds
import com.apptesters.data.dto.AppProgressResponseDto
import com.apptesters.data.dto.ProgressTesterDto
import com.apptesters.data.dto.TesterEmailDto
import com.apptesters.data.repository.ProgressRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owner-only screen. Updates live through Supabase Realtime while it is open. */
@Composable
fun LiveProgressScreen(
    appId: String,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    var data by remember { mutableStateOf<AppProgressResponseDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var emails by remember { mutableStateOf<List<TesterEmailDto>?>(null) }
    var removeTarget by remember { mutableStateOf<ProgressTesterDto?>(null) }

    suspend fun refresh() {
        val r = ProgressRepository().getAppProgress(appId)
        r.onSuccess { data = it; error = null }
        r.onFailure { if (data == null) error = it.userMessage() }
        loading = false
    }

    LaunchedEffect(appId) {
        refresh()
        while (true) {
            delay(60_000) // safety net; realtime below does the live updates
            refresh()
        }
    }

    LaunchedEffect(appId) {
        runCatching { RealtimeFeeds.progressChanges(appId).collect { refresh() } }
    }

    emails?.let { list ->
        AlertDialog(
            onDismissRequest = { emails = null },
            title = { Text("Tester emails") },
            text = {
                Text(if (list.isEmpty()) "No testers yet." else list.joinToString("\n") { it.playEmail })
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(list.joinToString(", ") { it.playEmail }))
                    emails = null
                    scope.launch { snackbar.showSnackbar("Emails copied. Paste them in your Google Group.") }
                }) { Text("Copy all") }
            },
            dismissButton = { TextButton(onClick = { emails = null }) { Text("Close") } }
        )
    }

    removeTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove ${t.displayName}?") },
            text = { Text("They will be taken off this test and notified.") },
            confirmButton = {
                TextButton(onClick = {
                    removeTarget = null
                    scope.launch {
                        val r = ProgressRepository().removeTester(appId, t.testerId)
                        if (r.isSuccess) { snackbar.showSnackbar("Removed ${t.displayName}."); refresh() }
                        else snackbar.showSnackbar(r.exceptionOrNull()?.userMessage() ?: "Failed.")
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancel") } }
        )
    }

    val c = Atc.colors
    val d = data
    val heroBg = Color(0xFF121212)
    val heroInk = Color(0xFFF3F1EA)
    val heroMuted = Color(0xFFB9B6AA)
    val segOff = if (c.isDark) Color(0xFF3A3A36) else Color(0xFF3A3A36)
    val ink = if (c.isDark) Lime else Color(0xFF121212)

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            AtcTopBar(onBack = onBack) {
                Box(Modifier.clip(CircleShape).clickable { scope.launch { loading = true; refresh() } }.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text("Refresh", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row(
                    Modifier.clip(CircleShape).background(Lime).border(1.5.dp, Color(0xFF121212), CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF121212)))
                    Spacer(Modifier.width(6.dp))
                    Text("Live", color = Color(0xFF121212), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
            }
            when {
                loading && d == null -> LoadingBox(Modifier.weight(1f))
                d == null -> ErrorBox(error ?: "Could not load.", onRetry = { loading = true; scope.launch { refresh() } }, modifier = Modifier.weight(1f))
                else -> Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Live progress", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 34.sp)

                    // Hero
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(heroBg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LetterAvatar(d.name, 40.dp, 12.dp, 18)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(d.name, color = heroInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                Text("Closed testing · ${d.status.replaceFirstChar { it.uppercase() }}", color = heroMuted, fontSize = 13.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
                                val frac = (d.joinedCount / d.testersNeeded.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                                Canvas(Modifier.fillMaxSize()) {
                                    val sw = 12.dp.toPx()
                                    val inset = sw / 2
                                    val s = Size(size.width - sw, size.height - sw)
                                    drawArc(segOff, 0f, 360f, false, Offset(inset, inset), s, style = Stroke(sw))
                                    drawArc(Lime, -90f, 360f * frac, false, Offset(inset, inset), s, style = Stroke(sw, cap = StrokeCap.Round))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text("${d.joinedCount}", color = heroInk, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 36.sp)
                                        Text("/${d.testersNeeded}", color = heroMuted, fontSize = 18.sp, modifier = Modifier.padding(bottom = 3.dp))
                                    }
                                    Text("testers", color = heroMuted, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.width(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column {
                                    Text("Testing period", color = heroMuted, fontSize = 13.sp)
                                    Text("Day ${d.qualifiedDays} / ${d.requiredDays}", color = Lime, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                                }
                                val need = (d.testersNeeded - d.joinedCount).coerceAtLeast(0)
                                Text(
                                    if (need > 0) "Need $need more testers to reach ${d.testersNeeded} before you can apply for production."
                                    else "You have all ${d.testersNeeded} testers. Keep them checking in daily.",
                                    color = heroMuted, fontSize = 13.sp, lineHeight = 18.sp
                                )
                            }
                        }
                        SegmentBar(d.qualifiedDays, d.requiredDays.coerceAtLeast(1), Lime, segOff)
                    }

                    // Stats
                    val missed = d.testers.count { it.status == "joined" && !it.checkedInToday }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            d.checkedInToday to "Active today",
                            missed to "Missed",
                            (d.testersNeeded - d.joinedCount).coerceAtLeast(0) to "Spots open"
                        ).forEach { (n, label) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("$n", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                                Text(label, color = c.muted, fontSize = 13.sp)
                            }
                        }
                    }

                    PillButton(
                        "Tester emails (for your Google Group)",
                        onClick = {
                            scope.launch {
                                val r = ProgressRepository().getTesterEmails(appId)
                                if (r.isSuccess) emails = r.getOrNull() ?: emptyList()
                                else snackbar.showSnackbar(r.exceptionOrNull()?.userMessage() ?: "Failed.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), height = 48.dp, outlined = true, content = c.ink
                    )

                    // Testers
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Your testers", "${d.joinedCount} joined")
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Legend(ink, null, "Checked in")
                            Legend(c.surface, ink, "Missed")
                            Legend(c.track, null, "Upcoming")
                        }
                        if (d.testers.isEmpty()) {
                            AtcCard { Text("Nobody has joined yet.", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(16.dp)) }
                        } else AtcCard {
                            d.testers.forEachIndexed { i, t ->
                                if (i > 0) HorizontalDivider(color = c.divider)
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!t.avatarUrl.isNullOrBlank()) AppIcon(t.avatarUrl, t.displayName, 40.dp)
                                        else Box(Modifier.size(40.dp).clip(CircleShape).background(tintFor(t.displayName)), contentAlignment = Alignment.Center) {
                                            Text(t.displayName.trim().take(2).uppercase(), color = Color(0xFF121212), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(t.displayName, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                                            Text(
                                                when {
                                                    t.status != "joined" -> t.status.replaceFirstChar { it.uppercase() }
                                                    t.checkedInToday -> "Checked in today · ${t.streak} day streak"
                                                    else -> "Missed today · streak at risk"
                                                },
                                                color = c.muted, fontSize = 13.sp
                                            )
                                        }
                                        if (t.status == "joined" && !t.checkedInToday) {
                                            PillButton("Nudge", {
                                                scope.launch {
                                                    val r = ProgressRepository().nudgeTester(appId, t.testerId)
                                                    snackbar.showSnackbar(if (r.isSuccess) "Nudged ${t.displayName}." else r.exceptionOrNull()?.userMessage() ?: "Failed to nudge.")
                                                }
                                            }, height = 44.dp, outlined = true, content = c.ink)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        val total = d.requiredDays.coerceAtLeast(14)
                                        repeat(total) { k ->
                                            val state = t.pattern.getOrNull(k)
                                            val m = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(3.dp))
                                            when (state) {
                                                true -> Box(m.background(ink))
                                                false -> Box(m.background(c.surface).border(2.dp, ink, RoundedCornerShape(3.dp)))
                                                null -> Box(m.background(c.track))
                                            }
                                        }
                                    }
                                    if (t.status == "joined") {
                                        Text(
                                            "Remove", color = c.danger, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                            modifier = Modifier.clickable { removeTarget = t }.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Legend(fill: Color, border: Color?, label: String) {
    val c = Atc.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(width = 14.dp, height = 10.dp).clip(RoundedCornerShape(3.dp)).background(fill)
                .then(if (border != null) Modifier.border(2.dp, border, RoundedCornerShape(3.dp)) else Modifier)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = c.muted, fontSize = 12.sp)
    }
}
