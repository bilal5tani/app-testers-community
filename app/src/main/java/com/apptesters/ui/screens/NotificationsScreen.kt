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
import com.apptesters.data.dto.NotificationDto
import com.apptesters.data.repository.InboxRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    val coroutineScope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = InboxRepository().listNotifications()
        notifications = result.getOrDefault(emptyList())
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications Inbox", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            InboxRepository().markRead()
                            val result = InboxRepository().listNotifications()
                            notifications = result.getOrDefault(emptyList())
                        }
                    }) {
                        Text("Mark All Read")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No notifications yet.", fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Event: ${item.event.replace('_', ' ').uppercase()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (!item.data.isNullOrBlank()) {
                                Text(item.data, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                            Text("Time: ${item.createdAt}", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
