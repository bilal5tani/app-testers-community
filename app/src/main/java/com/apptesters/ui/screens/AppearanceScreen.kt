package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptesters.data.repository.PreferencesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefsRepo = remember { PreferencesRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentTheme by prefsRepo.themeFlow.collectAsState(initial = "system")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.Bold) },
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Theme Mode")
                TextButton(onClick = {
                    coroutineScope.launch {
                        val nextTheme = when (currentTheme) {
                            "system" -> "light"
                            "light" -> "dark"
                            else -> "system"
                        }
                        prefsRepo.setTheme(nextTheme)
                    }
                }) {
                    Text(currentTheme.uppercase())
                }
            }
        }
    }
}
