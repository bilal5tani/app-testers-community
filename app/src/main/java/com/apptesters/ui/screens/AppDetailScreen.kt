package com.apptesters.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.dto.AppDetailResponseDto
import com.apptesters.data.repository.AppsRepository
import com.apptesters.data.repository.HomeRepository
import com.apptesters.data.repository.SettingsRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.launch

@Composable
fun AppDetailScreen(
    appId: String,
    onBack: () -> Unit = {},
    onNavigateToLiveProgress: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var detail by remember { mutableStateOf<AppDetailResponseDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) } // report_app | report_dev | block_dev | feedback

    suspend fun refresh() {
        val r = AppsRepository().getAppDetail(appId)
        r.onSuccess { detail = it; error = null }
        r.onFailure { if (detail == null) error = it.userMessage() }
        loading = false
    }

    fun act(ok: String, block: suspend () -> Result<*>) {
        scope.launch {
            busy = true
            val r = block()
            busy = false
            if (r.isSuccess) {
                snackbar.showSnackbar(ok)
                refresh()
            } else {
                snackbar.showSnackbar(r.exceptionOrNull()?.userMessage() ?: "Something went wrong.")
            }
        }
    }

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    LaunchedEffect(appId) { refresh() }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave this test?") },
            text = { Text("Your streak for this app will stop counting.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    act("You left the test.") { AppsRepository().leaveApp(appId) }
                }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancel") } }
        )
    }

    val current = detail
    when (dialog) {
        "report_app" -> ReasonDialog(
            title = "Report this app", label = "What is wrong?", submitLabel = "Report",
            onDismiss = { dialog = null },
            onSubmit = { reason ->
                dialog = null
                act("Thanks, we will review it.") { SettingsRepository().report("app", appId, reason) }
            }
        )
        "report_dev" -> ReasonDialog(
            title = "Report developer", label = "What is wrong?", submitLabel = "Report",
            onDismiss = { dialog = null },
            onSubmit = { reason ->
                dialog = null
                val ownerId = current?.owner?.id
                if (ownerId != null) act("Thanks, we will review it.") { SettingsRepository().report("user", ownerId, reason) }
            }
        )
        "block_dev" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Block this developer?") },
            text = { Text("You will no longer see each other's apps.") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    val ownerId = current?.owner?.id
                    if (ownerId != null) scope.launch {
                        val r = SettingsRepository().blockUser(ownerId)
                        if (r.isSuccess) onBack()
                        else snackbar.showSnackbar(r.exceptionOrNull()?.userMessage() ?: "Failed.")
                    }
                }) { Text("Block", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
        "feedback" -> FeedbackDialog(
            onDismiss = { dialog = null },
            onSubmit = { kind, body ->
                dialog = null
                act("Feedback sent to the developer.") { SettingsRepository().sendFeedback(appId, kind, body) }
            }
        )
    }

    val c = Atc.colors
    val d = detail
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            AtcTopBar(onBack = onBack) {
                if (d?.isOwner == false) {
                    Box {
                        Box(Modifier.size(44.dp).clip(CircleShape).clickable { menuOpen = true }, contentAlignment = Alignment.Center) {
                            AtcIcon(AtcIcons.More, c.ink, 22.dp)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Report this app") }, onClick = { menuOpen = false; dialog = "report_app" })
                            DropdownMenuItem(text = { Text("Report developer") }, onClick = { menuOpen = false; dialog = "report_dev" })
                            DropdownMenuItem(text = { Text("Block developer") }, onClick = { menuOpen = false; dialog = "block_dev" })
                        }
                    }
                }
            }
            when {
                loading -> LoadingBox(Modifier.weight(1f))
                d == null -> ErrorBox(error ?: "Could not load.", onRetry = { loading = true; scope.launch { refresh() } }, modifier = Modifier.weight(1f))
                else -> {
                    val app = d.app
                    val enr = d.myEnrollment
                    val joinedNow = enr != null && enr.status == "joined"
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        // Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app.iconUrl, app.title, 80.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(app.title, color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 30.sp)
                                Text("by ${d.owner.displayName} · ${app.category.replaceFirstChar { it.uppercase() }}", color = c.muted, fontSize = 14.sp)
                                Text(app.packageName, color = c.muted, fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        // Hero
                        Column(
                            Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(c.heroBg).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${app.joinedCount}", color = c.heroInk, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 44.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("of ${app.testersNeeded} testers joined", color = c.heroMuted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Spacer(Modifier.weight(1f))
                                Text("Day ${app.qualifiedDays} / 14", color = c.heroAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            SegmentBar(app.qualifiedDays, 14, c.heroSegOn, c.heroSegOff)
                            Text(
                                "${app.spotsLeft} spots left. Testing must run 14 days in a row for the developer to apply for production.",
                                color = c.heroMuted, fontSize = 13.sp
                            )
                        }

                        if (!app.description.isNullOrBlank()) {
                            Text(app.description, color = c.ink, fontSize = 14.sp, lineHeight = 20.sp)
                        }

                        // Your testing (joined)
                        if (joinedNow && enr != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionHeader("Your testing", "Streak ${enr.streak}")
                                AtcCard {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Day ${enr.daysDone} of 14", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        ThinBar(enr.daysDone / 14f)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            PillButton("Play Store page", { open(app.playStoreUrl) }, Modifier.weight(1f), height = 44.dp, outlined = true, content = c.ink)
                                            PillButton("Feedback", { dialog = "feedback" }, Modifier.weight(1f), height = 44.dp, outlined = true, content = c.ink)
                                        }
                                        PillButton("Leave test", { confirmLeave = true }, Modifier.fillMaxWidth(), height = 44.dp, outlined = true, content = c.danger, enabled = !busy)
                                    }
                                }
                            }
                        }

                        // How testing works
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader("How testing works")
                            AtcCard {
                                val steps = listOf(
                                    "Join the Google Group" to "The developer shares a group link. Joining it gives your Google account access to the test.",
                                    "Accept the testing invite" to "Open the Play Store opt-in link and tap Become a tester.",
                                    "Install and open the app" to "Download it from Google Play on your Android phone.",
                                    "Check in every day" to "Open the app once a day for 14 days and tap Check in here. Leave feedback whenever you find a bug."
                                )
                                steps.forEachIndexed { i, (t, b) ->
                                    if (i > 0) HorizontalDivider(color = c.divider)
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                        Box(Modifier.size(32.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                                            Text("${i + 1}", color = c.onAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(t, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(b, color = c.muted, fontSize = 13.sp, lineHeight = 18.sp)
                                            if (i == 0 && !d.isOwner) {
                                                Text(
                                                    "Open Google Group", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                                    modifier = Modifier.clickable { open(app.googleGroupUrl) }.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Testers so far
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader("Testers so far")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (d.testerFaces.isNotEmpty()) {
                                    Row {
                                        d.testerFaces.take(6).forEachIndexed { i, f ->
                                            val m = Modifier.offset(x = (-10 * i).dp)
                                            Box(m.size(40.dp).clip(CircleShape).border(2.dp, c.bg, CircleShape).background(tintFor(f.displayName)), contentAlignment = Alignment.Center) {
                                                if (!f.avatarUrl.isNullOrBlank()) AsyncImage(f.avatarUrl, f.displayName, Modifier.fillMaxSize())
                                                else Text(f.displayName.trim().take(2).uppercase(), color = Color(0xFF121212), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                }
                                Text(
                                    if (d.testerFaces.isEmpty()) "No testers yet. Be the first." else "Progress is shared live with the developer.",
                                    color = c.muted, fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Sticky CTA
                    Column(
                        Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = c.navBorder, modifier = Modifier.offset(y = (-12).dp).fillMaxWidth())
                        val ctaBg = if (c.isDark) c.accent else c.ink
                        val ctaFg = if (c.isDark) c.onAccent else c.accent
                        when {
                            d.isOwner -> PillButton("View live progress", { onNavigateToLiveProgress(app.id) }, Modifier.fillMaxWidth(), 54.dp, ctaBg, ctaFg)
                            joinedNow && enr != null -> {
                                PillButton(
                                    if (enr.checkedToday) "Checked in today ✓" else "Check in for today",
                                    { act("Checked in for today.") { HomeRepository().checkIn(app.id) } },
                                    Modifier.fillMaxWidth(), 54.dp, ctaBg, ctaFg, enabled = !busy && !enr.checkedToday
                                )
                                Text("Streak ${enr.streak} days. Check in once a day to keep it.", color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            enr != null -> Text("Your status for this app: ${enr.status}", color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            else -> {
                                PillButton(
                                    if (app.spotsLeft > 0) "Start testing" else "No spots left",
                                    { act("Joined! Now join the group and opt in on Play Store.") { AppsRepository().joinApp(app.id) } },
                                    Modifier.fillMaxWidth(), 54.dp, ctaBg, ctaFg,
                                    enabled = !busy && app.spotsLeft > 0 && app.status != "completed" && app.status != "paused"
                                )
                                Text("Free. You'll check in once a day to keep your streak.", color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
}
