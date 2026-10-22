package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.repository.PreferencesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefsRepo = remember { PreferencesRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val currentTheme by prefsRepo.themeFlow.collectAsState(initial = "system")
    val currentLanguage by prefsRepo.languageFlow.collectAsState(initial = "en")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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

            HorizontalDivider()

            Text("Language", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("App Language")
                TextButton(onClick = {
                    coroutineScope.launch {
                        val nextLang = if (currentLanguage == "en") "ur" else "en"
                        prefsRepo.setLanguage(nextLang)
                    }
                }) {
                    Text(if (currentLanguage == "en") "English" else "Urdu (اردو)")
                }
            }
        }
    }
}
