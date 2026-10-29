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
import com.apptesters.data.dto.ProfileResponseDto
import com.apptesters.data.repository.ProfileRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToAppDetail: (String) -> Unit = {}
) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = { TextButton(onClick = onNavigateToSettings) { Text("Settings") } }
            )
        }
    ) { innerPadding ->
        val d = data
        when {
            loading -> LoadingBox(Modifier.padding(innerPadding))
            d == null -> ErrorBox(error ?: "Could not load.", onRetry = { loading = true; scope.launch { load() } }, modifier = Modifier.padding(innerPadding))
            else -> {
                val p = d.profile
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(p.avatarUrl, p.displayName, 56.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(p.displayName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        Text("@${p.username ?: ""}", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                if (!p.bio.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(p.bio, fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = onNavigateToEditProfile, modifier = Modifier.fillMaxWidth()) {
                                    Text("Edit Profile")
                                }
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("Apps tested", d.stats.appsTested, Modifier.weight(1f))
                            StatCard("Karma", p.karma, Modifier.weight(1f))
                            StatCard("Best streak", d.stats.bestStreak, Modifier.weight(1f))
                            StatCard("Testers got", d.stats.testersReceived, Modifier.weight(1f))
                        }
                    }

                    if (d.myApps.isNotEmpty()) {
                        item { Text("My apps", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        items(d.myApps, key = { it.id }) { app ->
                            OutlinedCard(onClick = { onNavigateToAppDetail(app.id) }, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(app.iconUrl, app.title, 40.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(app.title, fontWeight = FontWeight.Bold)
                                        Text("${app.status.uppercase()} • ${app.qualifiedDays}/14 days", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (d.history.isNotEmpty()) {
                        item { Text("Testing history", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        items(d.history, key = { it.appId }) { h ->
                            OutlinedCard(onClick = { onNavigateToAppDetail(h.appId) }, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(h.iconUrl, h.title, 40.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(h.title, fontWeight = FontWeight.Bold)
                                        Text("${h.status.uppercase()} • ${h.daysDone} days done", fontSize = 12.sp)
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

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, fontSize = 11.sp)
        }
    }
}
