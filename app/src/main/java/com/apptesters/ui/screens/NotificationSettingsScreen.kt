package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        if (!loaded) {
            LoadingBox(Modifier.padding(innerPadding))
        } else {
            Column(
                Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("As a tester", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                PrefRow("Daily check-in reminder", "Around ${prefs.remindAt.take(5)} (Pakistan time)", prefs.remind) {
                    save("remind", it) { p -> p.copy(remind = it) }
                }
                PrefRow("Streak at risk", "When you are about to miss a day", prefs.streakRisk) {
                    save("streak_risk", it) { p -> p.copy(streakRisk = it) }
                }
                PrefRow("New apps needing testers", null, prefs.newApps) {
                    save("new_apps", it) { p -> p.copy(newApps = it) }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("As an app owner", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                PrefRow("Tester joined or left", null, prefs.testerJoined) {
                    save("tester_joined", it) { p -> p.copy(testerJoined = it) }
                }
                PrefRow("Tester missed check-ins", null, prefs.testerMissed) {
                    save("tester_missed", it) { p -> p.copy(testerMissed = it) }
                }
                PrefRow("Reached the tester target", null, prefs.reachedTarget) {
                    save("reached_target", it) { p -> p.copy(reachedTarget = it) }
                }
                PrefRow("14-day period complete", null, prefs.periodComplete) {
                    save("period_complete", it) { p -> p.copy(periodComplete = it) }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                PrefRow("Weekly summary", null, prefs.weekly) {
                    save("weekly", it) { p -> p.copy(weekly = it) }
                }
            }
        }
    }
}

@Composable
private fun PrefRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
