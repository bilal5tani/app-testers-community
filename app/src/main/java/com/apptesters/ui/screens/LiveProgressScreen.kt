package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.dto.AppProgressResponseDto
import com.apptesters.data.repository.ProgressRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveProgressScreen(
    appId: String,
    onBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var progressData by remember { mutableStateOf<AppProgressResponseDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var nudgeMessage by remember { mutableStateOf<String?>(null) }

    fun loadProgress() {
        coroutineScope.launch {
            isLoading = true
            val result = ProgressRepository().getAppProgress(appId)
            if (result.isSuccess) {
                progressData = result.getOrNull()
            }
            isLoading = false
        }
    }

    LaunchedEffect(appId) {
        loadProgress()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Progress & Testers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val data = progressData
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Testing Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            val qDays = data?.qualifiedDays ?: 0
                            LinearProgressIndicator(
                                progress = { qDays / 14f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Qualified Days: $qDays / 14", fontSize = 14.sp)
                            Text("Testers Joined: ${data?.joinedCount ?: 0} / ${data?.testersNeeded ?: 12}", fontSize = 14.sp)
                        }
                    }
                }

                item {
                    Text("Enrolled Testers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                if (nudgeMessage != null) {
                    item {
                        Text(nudgeMessage!!, color = MaterialTheme.colorScheme.primary)
                    }
                }

                items(data?.testers ?: emptyList()) { tester ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tester.displayName, fontWeight = FontWeight.Bold)
                                Text("Streak: ${tester.streak} days • Status: ${tester.status.uppercase()}", fontSize = 12.sp)
                                Text(if (tester.checkedInToday) "Checked in today ✓" else "Not checked in today", fontSize = 11.sp, color = if (tester.checkedInToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                            if (!tester.checkedInToday) {
                                Button(onClick = {
                                    coroutineScope.launch {
                                        val res = ProgressRepository().nudgeTester(appId, tester.testerId)
                                        if (res.isSuccess) {
                                            nudgeMessage = "Nudged ${tester.displayName} successfully!"
                                            loadProgress()
                                        } else {
                                            nudgeMessage = res.exceptionOrNull()?.message ?: "Failed to nudge."
                                        }
                                    }
                                }) {
                                    Text("Nudge")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
