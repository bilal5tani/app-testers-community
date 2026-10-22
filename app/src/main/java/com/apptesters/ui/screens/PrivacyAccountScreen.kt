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
import com.apptesters.data.AuthRepository
import com.apptesters.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAccountScreen(
    onBack: () -> Unit = {},
    onSignedOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPublic by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Account", fontWeight = FontWeight.Bold) },
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
                Text("Public Profile")
                Switch(checked = isPublic, onCheckedChange = { isPublic = it })
            }

            HorizontalDivider()

            if (statusMessage != null) {
                Text(statusMessage!!, color = MaterialTheme.colorScheme.primary)
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            SupabaseClientProvider.client.postgrest.rpc("rpc_export_data")
                            statusMessage = "Data export requested."
                        } catch (e: Exception) {
                            statusMessage = e.message ?: "Export failed"
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export My Data")
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            SupabaseClientProvider.client.postgrest.rpc("rpc_delete_account")
                            AuthRepository(context).signOut()
                            onSignedOut()
                        } catch (e: Exception) {
                            statusMessage = e.message ?: "Delete account failed"
                        }
                        isLoading = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Account")
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        AuthRepository(context).signOut()
                        onSignedOut()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
}
