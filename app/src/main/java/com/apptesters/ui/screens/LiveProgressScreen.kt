package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.apptesters.data.dto.AppProgressResponseDto
import com.apptesters.data.dto.ProgressTesterDto
import com.apptesters.data.dto.TesterEmailDto
import com.apptesters.data.repository.ProgressRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owner-only screen. Refreshes itself every 20 seconds while it is open. */
@OptIn(ExperimentalMaterial3Api::class)
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
            delay(20_000)
            refresh()
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Progress", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { scope.launch { loading = true; refresh() } }) { Text("Refresh") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        val d = data
        when {
            loading && d == null -> LoadingBox(Modifier.padding(innerPadding))
            d == null -> ErrorBox(error ?: "Could not load.", onRetry = {
                loading = true
                scope.launch { refresh() }
            }, modifier = Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(d.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Status: ${d.status.uppercase()}", fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { d.qualifiedDays / d.requiredDays.coerceAtLeast(1).toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Qualified days: ${d.qualifiedDays} / ${d.requiredDays}", fontSize = 14.sp)
                            Text("Testers opted in: ${d.joinedCount} / ${d.testersNeeded}", fontSize = 14.sp)
                            Text("Checked in today: ${d.checkedInToday}", fontSize = 14.sp)
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val r = ProgressRepository().getTesterEmails(appId)
                                if (r.isSuccess) emails = r.getOrNull() ?: emptyList()
                                else snackbar.showSnackbar(r.exceptionOrNull()?.userMessage() ?: "Failed.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Tester emails (for your Google Group)") }
                }

                item { Text("Testers", fontWeight = FontWeight.Bold, fontSize = 18.sp) }

                if (d.testers.isEmpty()) {
                    item { Text("Nobody has joined yet.", color = MaterialTheme.colorScheme.secondary) }
                }

                items(d.testers, key = { it.testerId }) { t ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(t.avatarUrl, t.displayName, 36.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.displayName, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Day ${t.daysDone} • Streak ${t.streak} • ${t.status.uppercase()}",
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    if (t.checkedInToday) "Today ✓" else "Today –",
                                    fontSize = 12.sp,
                                    color = if (t.checkedInToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                            if (t.pattern.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    t.pattern.forEach { done ->
                                        Box(
                                            Modifier
                                                .size(width = 14.dp, height = 14.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (done) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant
                                                )
                                        )
                                    }
                                }
                            }
                            if (t.status == "joined") {
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (!t.checkedInToday) {
                                        TextButton(onClick = {
                                            scope.launch {
                                                val r = ProgressRepository().nudgeTester(appId, t.testerId)
                                                snackbar.showSnackbar(
                                                    if (r.isSuccess) "Nudged ${t.displayName}."
                                                    else r.exceptionOrNull()?.userMessage() ?: "Failed to nudge."
                                                )
                                            }
                                        }) { Text("Nudge") }
                                    }
                                    TextButton(onClick = { removeTarget = t }) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error)
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
