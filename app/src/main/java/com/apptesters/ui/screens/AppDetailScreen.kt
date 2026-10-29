package com.apptesters.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import com.apptesters.domain.userMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.app?.title ?: "App Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        val d = detail
        when {
            loading -> LoadingBox(Modifier.padding(innerPadding))
            d == null -> ErrorBox(error ?: "Could not load.", onRetry = {
                loading = true
                scope.launch { refresh() }
            }, modifier = Modifier.padding(innerPadding))
            else -> {
                val app = d.app
                val enr = d.myEnrollment
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(app.iconUrl, app.title, 56.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(app.title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                    Text(app.packageName, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text(
                                        "${app.category.replaceFirstChar { it.uppercase() }} • ${app.status.replaceFirstChar { it.uppercase() }}",
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                LinearProgressIndicator(
                                    progress = { app.qualifiedDays / 14f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("${app.qualifiedDays} / 14 qualified days", fontSize = 13.sp)
                                Text("${app.joinedCount} / ${app.testersNeeded} testers opted in • ${app.spotsLeft} spots left", fontSize = 13.sp)
                                if (!app.description.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(app.description, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Developer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(d.owner.displayName, fontSize = 16.sp)
                                Text("Karma ${d.owner.karma}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    if (d.isOwner) {
                        item {
                            Button(onClick = { onNavigateToLiveProgress(app.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("View Live Progress")
                            }
                        }
                    } else if (enr != null && enr.status == "joined") {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Your testing", fontWeight = FontWeight.Bold)
                                    Text("Day ${enr.daysDone} of 14 • Streak ${enr.streak}", fontSize = 14.sp)
                                    Button(
                                        onClick = { act("Checked in for today.") { HomeRepository().checkIn(app.id) } },
                                        enabled = !busy && !enr.checkedToday,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(if (enr.checkedToday) "Checked in today ✓" else "Check in for today") }
                                    OutlinedButton(onClick = { open(app.playStoreUrl) }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Open Play Store test page")
                                    }
                                    OutlinedButton(
                                        onClick = { confirmLeave = true },
                                        enabled = !busy,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Leave test") }
                                }
                            }
                        }
                    } else if (enr != null) {
                        item {
                            Text("Your status for this app: ${enr.status}", color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("How to test", fontWeight = FontWeight.Bold)
                                    Text("1. Tap Join below.\n2. Join the developer's Google Group.\n3. Open the Play Store test link and opt in.\n4. Install the app and check in every day for 14 days.", fontSize = 14.sp)
                                    OutlinedButton(onClick = { open(app.googleGroupUrl) }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Open Google Group")
                                    }
                                    Button(
                                        onClick = {
                                            act("Joined! Now join the group and opt in on Play Store.") {
                                                AppsRepository().joinApp(app.id)
                                            }
                                        },
                                        enabled = !busy && app.spotsLeft > 0 && app.status != "completed" && app.status != "paused",
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (busy) CircularProgressIndicator(Modifier.size(20.dp))
                                        else Text(if (app.spotsLeft > 0) "Join this test" else "No spots left")
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
