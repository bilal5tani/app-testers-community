package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptesters.data.repository.AppsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppScreen(onAppCreated: () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var playStoreUrl by remember { mutableStateOf("") }
    var googleGroupUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("List Your App", fontWeight = FontWeight.Bold) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("App Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it },
                label = { Text("Package Name (e.g. com.example.app)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = playStoreUrl,
                onValueChange = { playStoreUrl = it },
                label = { Text("Play Store Opt-In Link") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = googleGroupUrl,
                onValueChange = { googleGroupUrl = it },
                label = { Text("Google Group Join Link") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        val result = AppsRepository().createApp(
                            title = title,
                            packageName = packageName,
                            playStoreUrl = playStoreUrl,
                            googleGroupUrl = googleGroupUrl,
                            description = description
                        )
                        isLoading = false
                        if (result.isSuccess) {
                            onAppCreated()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to create app."
                        }
                    }
                },
                enabled = !isLoading && title.isNotBlank() && packageName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Submit App")
            }
        }
    }
}
