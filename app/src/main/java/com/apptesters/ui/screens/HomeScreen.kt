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
import com.apptesters.data.dto.HomeResponseDto
import com.apptesters.data.repository.FakeHomeRepository
import com.apptesters.data.repository.HomeRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var homeData by remember { mutableStateOf<HomeResponseDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = HomeRepository().getHome()
        if (result.isSuccess) {
            homeData = result.getOrNull()
        } else {
            homeData = FakeHomeRepository().getHome().getOrNull()
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Testers Community", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val data = homeData
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val myApp = data?.myApp
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("My Listed App", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (myApp != null) {
                                Text(myApp.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("Status: ${myApp.status.uppercase()}", fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { myApp.qualifiedDays / 14f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Qualified Days: ${myApp.qualifiedDays} / 14 days", fontSize = 12.sp)
                            } else {
                                Text("You have no active listed app.", fontSize = 16.sp)
                            }
                        }
                    }
                }

                item {
                    Text("Today's Check-ins", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                items(data?.todayCheckins ?: emptyList()) { checkIn ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(checkIn.title, fontWeight = FontWeight.Bold)
                                Text("Streak: ${checkIn.streak} days", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        HomeRepository().checkIn(checkIn.appId)
                                    }
                                },
                                enabled = !checkIn.checkedInToday
                            ) {
                                Text(if (checkIn.checkedInToday) "Checked In ✓" else "Check In")
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Apps Needing Testers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        TextButton(onClick = onNavigateToDiscover) {
                            Text("See All")
                        }
                    }
                }

                items(data?.needsTesters ?: emptyList()) { app ->
                    OutlinedCard(
                        onClick = { onNavigateToAppDetail(app.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(app.title, fontWeight = FontWeight.Bold)
                            Text("By ${app.ownerName} • ${app.spotsLeft} spots left", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
