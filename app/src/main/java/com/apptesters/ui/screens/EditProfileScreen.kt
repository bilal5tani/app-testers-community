package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptesters.data.repository.ProfileRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var playEmail by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var androidVersion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = ProfileRepository().getProfile()
        if (result.isSuccess) {
            val data = result.getOrNull()
            username = data?.profile?.username ?: ""
            displayName = data?.profile?.displayName ?: ""
            bio = data?.profile?.bio ?: ""
            playEmail = data?.private?.playEmail ?: ""
            deviceModel = data?.private?.deviceModel ?: ""
            androidVersion = data?.private?.androidVersion ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = playEmail,
                onValueChange = { playEmail = it },
                label = { Text("Google Play Email") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = deviceModel,
                onValueChange = { deviceModel = it },
                label = { Text("Device Model") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = androidVersion,
                onValueChange = { androidVersion = it },
                label = { Text("Android Version") },
                modifier = Modifier.fillMaxWidth()
            )

            if (successMessage != null) {
                Text(successMessage!!, color = MaterialTheme.colorScheme.primary)
            }
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        successMessage = null
                        errorMessage = null
                        val res = ProfileRepository().updateProfile(
                            username = username,
                            displayName = displayName,
                            bio = bio,
                            playEmail = playEmail,
                            deviceModel = deviceModel,
                            androidVersion = androidVersion
                        )
                        isLoading = false
                        if (res.isSuccess) {
                            successMessage = "Profile updated successfully!"
                        } else {
                            errorMessage = res.exceptionOrNull()?.message ?: "Update failed"
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Save Changes")
            }
        }
    }
}
