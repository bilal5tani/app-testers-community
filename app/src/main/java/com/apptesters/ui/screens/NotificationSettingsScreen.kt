package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.dto.NotificationPrefsDto
import com.apptesters.data.repository.SettingsRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.launch

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var prefs by remember { mutableStateOf(NotificationPrefsDto()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // an empty update returns the current row
        SettingsRepository().savePrefs(emptyMap()).onSuccess { prefs = it }
        loaded = true
    }

    fun save(key: String, value: Boolean, apply: (NotificationPrefsDto) -> NotificationPrefsDto) {
        val old = prefs
        prefs = apply(old)
        scope.launch {
            val r = SettingsRepository().savePrefs(mapOf(key to value))
            r.onSuccess { prefs = it }
            r.onFailure {
                prefs = old
                snackbar.showSnackbar(it.userMessage())
            }
        }
    }

    val c = Atc.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            AtcTopBar(onBack = onBack) {}
            if (!loaded) LoadingBox(Modifier.weight(1f)) else Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Text("Notifications", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(start = 4.dp))

                @Composable
                fun Group(title: String, rows: @Composable ColumnScope.() -> Unit) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { GroupLabel(title); AtcCard(content = rows) }
                }

                Group("When I am testing") {
                    ToggleRow("Daily check-in reminder", "Around ${prefs.remindAt.take(5)} (Pakistan time)", prefs.remind) { save("remind", it) { p -> p.copy(remind = it) } }
                    HorizontalDivider(color = c.divider)
                    ToggleRow("Streak at risk", "Before you lose a day of your 14-day run", prefs.streakRisk) { save("streak_risk", it) { p -> p.copy(streakRisk = it) } }
                    HorizontalDivider(color = c.divider)
                    ToggleRow("New apps needing testers", "When developers add apps that need testers", prefs.newApps) { save("new_apps", it) { p -> p.copy(newApps = it) } }
                }
                Group("When I am a developer") {
                    ToggleRow("Tester joined or left", "Someone opted in to or left your test", prefs.testerJoined) { save("tester_joined", it) { p -> p.copy(testerJoined = it) } }
                    HorizontalDivider(color = c.divider)
                    ToggleRow("Tester missed check-ins", "So you can send a nudge", prefs.testerMissed) { save("tester_missed", it) { p -> p.copy(testerMissed = it) } }
                    HorizontalDivider(color = c.divider)
                    ToggleRow("Reached the tester target", "Your app has enough testers", prefs.reachedTarget) { save("reached_target", it) { p -> p.copy(reachedTarget = it) } }
                    HorizontalDivider(color = c.divider)
                    ToggleRow("14-day period complete", "After 14 days in a row", prefs.periodComplete) { save("period_complete", it) { p -> p.copy(periodComplete = it) } }
                }
                Group("Summary") {
                    ToggleRow("Weekly summary", "One message with your progress", prefs.weekly) { save("weekly", it) { p -> p.copy(weekly = it) } }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
