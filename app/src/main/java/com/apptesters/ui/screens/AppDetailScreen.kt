package com.apptesters.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appId: String,
    onBack: () -> Unit = {},
    onNavigateToLiveProgress: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<AppDetailResponseDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadDetail() {
        coroutineScope.launch {
            isLoading = true
            val result = AppsRepository().getAppDetail(appId)
            if (result.isSuccess) {
                detail = result.getOrNull()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to load app detail."
            }
            isLoading = false
        }
    }

    LaunchedEffect(appId) {
        loadDetail()
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
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val app = detail?.app
            val owner = detail?.owner
            val enrollment = detail?.myEnrollment

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (app != null) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(app.title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Package: ${app.packageName}", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Category: ${app.category.uppercase()} • Status: ${app.status.uppercase()}", fontSize = 14.sp)
                                Text("Spots Left: ${app.spotsLeft} / ${app.testersNeeded}", fontSize = 14.sp)
                                if (!app.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(app.description, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    if (owner != null) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Developer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(owner.displayName, fontSize = 16.sp)
                                    Text("Karma: ${owner.karma}", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!app.playStoreUrl.isBlank()) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.playStoreUrl))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open Play Store Opt-In")
                                }
                            }
                            if (!app.googleGroupUrl.isBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.googleGroupUrl))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Join Google Group")
                                }
                            }
                        }
                    }

                    item {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    item {
                        if (enrollment != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onNavigateToLiveProgress(app.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View Live Progress")
                                }
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            actionLoading = true
                                            val res = AppsRepository().leaveApp(app.id)
                                            actionLoading = false
                                            if (res.isSuccess) loadDetail()
                                            else errorMessage = res.exceptionOrNull()?.message
                                        }
                                    },
                                    enabled = !actionLoading,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Leave Testing")
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        actionLoading = true
                                        val res = AppsRepository().joinApp(app.id)
                                        actionLoading = false
                                        if (res.isSuccess) loadDetail()
                                        else errorMessage = res.exceptionOrNull()?.message
                                    }
                                },
                                enabled = !actionLoading && app.spotsLeft > 0,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (actionLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                else Text("Join Testing Program")
                            }
                        }
                    }
                }
            }
        }
    }
}
