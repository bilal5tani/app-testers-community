package com.apptesters.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptesters.data.AuthRepository
import com.apptesters.data.SupabaseClientProvider
import com.apptesters.data.repository.ProfileRepository
import com.apptesters.domain.userMessage
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
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
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ProfileRepository().getProfile().getOrNull()?.let { isPublic = it.profile.isPublic }
    }

    fun signOutNow() {
        coroutineScope.launch {
            runCatching { AuthRepository(context).signOut() }
            onSignedOut()
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete account?") },
            text = {
                Text("Your profile, listed apps, check-ins and uploads will be permanently deleted. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    coroutineScope.launch {
                        isLoading = true
                        statusMessage = null
                        try {
                            SupabaseClientProvider.client.functions.invoke("delete-account")
                            runCatching { AuthRepository(context).signOut() }
                            onSignedOut()
                        } catch (e: Exception) {
                            isError = true
                            statusMessage = e.userMessage()
                        }
                        isLoading = false
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Public Profile")
                Switch(
                    checked = isPublic,
                    onCheckedChange = { value ->
                        val previous = isPublic
                        isPublic = value
                        coroutineScope.launch {
                            val res = ProfileRepository().updateProfile(isPublic = value)
                            if (res.isFailure) {
                                isPublic = previous
                                isError = true
                                statusMessage = res.exceptionOrNull()?.userMessage()
                            }
                        }
                    }
                )
            }

            HorizontalDivider()

            statusMessage?.let {
                Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        statusMessage = null
                        try {
                            val json = SupabaseClientProvider.client.functions.invoke("export-data").bodyAsText()
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "App Testers Community - my data")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(send, "Save or share my data"))
                            isError = false
                            statusMessage = "Your data is ready."
                        } catch (e: Exception) {
                            isError = true
                            statusMessage = e.userMessage()
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export My Data")
            }

            Button(
                onClick = { confirmDelete = true },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Account")
            }

            OutlinedButton(
                onClick = { signOutNow() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
}
