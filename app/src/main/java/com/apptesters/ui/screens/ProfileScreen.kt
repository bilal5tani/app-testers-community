package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.dto.ProfileResponseDto
import com.apptesters.data.repository.ProfileRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {}
) {
    var profileData by remember { mutableStateOf<ProfileResponseDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = ProfileRepository().getProfile()
        if (result.isSuccess) {
            profileData = result.getOrNull()
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onNavigateToSettings) {
                        Text("Settings")
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
            val p = profileData?.profile
            val stats = profileData?.stats
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(p?.displayName ?: "Guest User", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("@${p?.username ?: "guest"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                        if (!p?.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(p!!.bio, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onNavigateToEditProfile, modifier = Modifier.fillMaxWidth()) {
                            Text("Edit Profile")
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Card(modifier = Modifier.weight(1f).padding(4.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats?.appsTested ?: 0}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Apps Tested", fontSize = 12.sp)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).padding(4.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${p?.karma ?: 0}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Karma", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
