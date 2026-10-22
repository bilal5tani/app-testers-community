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
import com.apptesters.data.dto.DiscoverAppDto
import com.apptesters.data.repository.AppsRepository
import com.apptesters.data.repository.FakeAppsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(onNavigateToAppDetail: (String) -> Unit = {}) {
    var searchQuery by remember { mutableStateOf("") }
    var appsList by remember { mutableStateOf<List<DiscoverAppDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(searchQuery) {
        val result = AppsRepository().discover(query = searchQuery)
        if (result.isSuccess) {
            appsList = result.getOrDefault(emptyList())
        } else {
            appsList = FakeAppsRepository().discover().getOrDefault(emptyList())
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Discover Apps", fontWeight = FontWeight.Bold) })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search apps...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(appsList) { app ->
                        Card(
                            onClick = { onNavigateToAppDetail(app.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(app.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Category: ${app.category} • Spots left: ${app.spotsLeft}", fontSize = 14.sp)
                                Text("Owner: ${app.ownerName} (Karma: ${app.ownerKarma})", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                if (!app.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(app.description, fontSize = 12.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
