package com.apptesters.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListItem(
                headlineContent = { Text("Edit Profile", fontWeight = FontWeight.Medium, fontSize = 16.sp) },
                modifier = Modifier.clickable(onClick = onNavigateToEditProfile)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Notification Settings", fontWeight = FontWeight.Medium, fontSize = 16.sp) },
                modifier = Modifier.clickable(onClick = onNavigateToNotificationSettings)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Appearance (Theme)", fontWeight = FontWeight.Medium, fontSize = 16.sp) },
                modifier = Modifier.clickable(onClick = onNavigateToAppearance)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Privacy & Account", fontWeight = FontWeight.Medium, fontSize = 16.sp) },
                modifier = Modifier.clickable(onClick = onNavigateToPrivacy)
            )
        }
    }
}
