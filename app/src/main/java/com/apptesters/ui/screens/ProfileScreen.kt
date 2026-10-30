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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.dto.ProfileResponseDto
import com.apptesters.data.repository.ProfileRepository
import com.apptesters.domain.userMessage
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import com.apptesters.ui.theme.Lime
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val c = Atc.colors
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<ProfileResponseDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        val r = ProfileRepository().getProfile()
        r.onSuccess { data = it; error = null }
        r.onFailure { error = it.userMessage() }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    Box(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        val d = data
        when {
            loading -> LoadingBox()
            d == null -> ErrorBox(error ?: "Could not load.", onRetry = { loading = true; scope.launch { load() } })
            else -> {
                val p = d.profile
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!p.avatarUrl.isNullOrBlank()) coil.compose.AsyncImage(
                            p.avatarUrl, p.displayName, Modifier.size(76.dp).clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        ) else InitialsCircle(p.displayName, 76.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(p.displayName, color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 30.sp, maxLines = 1)
                            Text("@${p.username ?: ""} · Karma ${p.karma}", color = c.muted, fontSize = 14.sp)
                            if (!p.bio.isNullOrBlank()) Text(p.bio, color = c.muted, fontSize = 14.sp, maxLines = 2)
                        }
                        CircleIconButton(AtcIcons.Sliders, onNavigateToSettings)
                    }
                    PillButton("Edit profile", onNavigateToEditProfile, Modifier.fillMaxWidth(), 48.dp, outlined = true, content = c.ink)

                    // Stats
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val ink = Color(0xFF121212)
                        Stat(d.stats.appsTested, "Apps tested", ink, Lime, Modifier.weight(1f))
                        Stat(d.stats.bestStreak, "Best streak", Lime, ink, Modifier.weight(1f))
                        Stat(d.stats.testersReceived, "Testers got", c.surface, c.ink, Modifier.weight(1f))
                    }

                    // My apps
                    if (d.myApps.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("My apps")
                        AtcCard {
                            d.myApps.forEachIndexed { i, app ->
                                if (i > 0) HorizontalDivider(color = c.divider)
                                Row(Modifier.clickable { onNavigateToAppDetail(app.id) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(app.iconUrl, app.title, 44.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(app.title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                                        Text("${app.status.replaceFirstChar { it.uppercase() }} · Day ${app.qualifiedDays} of 14", color = c.muted, fontSize = 13.sp)
                                    }
                                    AtcIcon(AtcIcons.Chevron, c.muted, 20.dp)
                                }
                            }
                        }
                    }

                    // Testing setup
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Testing setup")
                        AtcCard {
                            listOf(
                                "Play account email" to d.private?.playEmail,
                                "Test device" to d.private?.deviceModel,
                                "Android version" to d.private?.androidVersion
                            ).forEachIndexed { i, (label, value) ->
                                if (i > 0) HorizontalDivider(color = c.divider)
                                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, color = c.muted, fontSize = 14.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(value?.ifBlank { null } ?: "Not set", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End, maxLines = 1)
                                }
                            }
                        }
                        Text("Developers use your Play account email to add you to their test group. It's only shared when you join a test.", color = c.muted, fontSize = 13.sp, lineHeight = 18.sp)
                    }

                    // History
                    if (d.history.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Recently tested")
                        AtcCard {
                            d.history.forEachIndexed { i, h ->
                                if (i > 0) HorizontalDivider(color = c.divider)
                                val done = h.status == "completed"
                                Row(Modifier.clickable { onNavigateToAppDetail(h.appId) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(h.iconUrl, h.title, 44.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(h.title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                                        Text("${h.daysDone} of 14 days", color = c.muted, fontSize = 13.sp)
                                    }
                                    Text(
                                        if (done) "Completed" else h.status.replaceFirstChar { it.uppercase() },
                                        color = if (done) Color(0xFF121212) else c.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        modifier = Modifier.clip(CircleShape).background(if (done) Lime else Color.Transparent)
                                            .border(1.5.dp, if (done) Lime else if (c.isDark) c.line else Color(0xFF121212), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: Int, label: String, bg: Color, fg: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$value", color = fg, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Text(label, color = fg.copy(alpha = 0.85f), fontSize = 13.sp)
    }
}
