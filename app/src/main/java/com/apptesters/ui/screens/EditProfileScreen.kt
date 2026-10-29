package com.apptesters.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptesters.data.repository.ProfileRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.launch

private val USERNAME_REGEX = Regex("^[a-z0-9._]{3,20}$")

/**
 * firstRun = true: shown right after the first sign-in. Username is required and
 * there is no back button; onSaved is called once the profile is saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit = {},
    firstRun: Boolean = false,
    onSaved: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var playEmail by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var androidVersion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = ProfileRepository().getProfile()
        result.getOrNull()?.let { data ->
            username = data.profile.username ?: ""
            displayName = data.profile.displayName
            bio = data.profile.bio ?: ""
            playEmail = data.private?.playEmail ?: ""
            deviceModel = data.private?.deviceModel ?: ""
            androidVersion = data.private?.androidVersion ?: ""
        }
        if (result.isFailure) errorMessage = result.exceptionOrNull()?.userMessage()
        if (deviceModel.isBlank()) deviceModel = Build.MODEL ?: ""
        if (androidVersion.isBlank()) androidVersion = Build.VERSION.RELEASE ?: ""
        loaded = true
    }

    val usernameClean = username.trim().lowercase()
    val usernameValid = USERNAME_REGEX.matches(usernameClean)
    val canSave = loaded && !isLoading && displayName.isNotBlank() &&
        (usernameValid || (!firstRun && usernameClean.isEmpty()))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (firstRun) "Set up your profile" else "Edit Profile", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (!firstRun) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (firstRun) {
                Text("Choose a username so other testers can find you.")
            }
            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= 40) displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' }.take(20) },
                label = { Text("Username") },
                supportingText = { Text("3-20 characters: a-z, 0-9, dot, underscore") },
                isError = username.isNotEmpty() && !usernameValid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 160) bio = it },
                label = { Text("Bio") },
                supportingText = { Text("${bio.length}/160") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = playEmail,
                onValueChange = { playEmail = it },
                label = { Text("Google Play Email") },
                supportingText = { Text("Shown only to owners of apps you test") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = deviceModel,
                onValueChange = { deviceModel = it },
                label = { Text("Device Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = androidVersion,
                onValueChange = { androidVersion = it },
                label = { Text("Android Version") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        val res = ProfileRepository().updateProfile(
                            username = usernameClean.ifEmpty { null },
                            displayName = displayName.trim(),
                            bio = bio.trim(),
                            playEmail = playEmail.trim().ifEmpty { null },
                            deviceModel = deviceModel.trim().ifEmpty { null },
                            androidVersion = androidVersion.trim().ifEmpty { null }
                        )
                        isLoading = false
                        if (res.isSuccess) onSaved()
                        else errorMessage = res.exceptionOrNull()?.userMessage() ?: "Update failed"
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text(if (firstRun) "Continue" else "Save Changes")
            }
        }
    }
}
