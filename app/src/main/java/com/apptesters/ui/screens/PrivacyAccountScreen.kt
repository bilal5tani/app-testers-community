package com.apptesters.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.apptesters.data.AuthRepository
import com.apptesters.data.SupabaseClientProvider
import com.apptesters.data.repository.ProfileRepository
import com.apptesters.domain.userMessage
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

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
                }) { Text("Delete", color = Atc.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    val c = Atc.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        AtcTopBar(onBack = onBack) {}
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Text("Privacy & account", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(start = 4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GroupLabel("Visibility")
                AtcCard {
                    ToggleRow("Public profile", "Other testers and developers can see your profile", isPublic) { value ->
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
                }
            }

            statusMessage?.let { Text(it, color = if (isError) c.danger else c.ink, fontSize = 14.sp) }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GroupLabel("Your data")
                AtcCard {
                    Column(
                        Modifier.fillMaxWidth().clickable(enabled = !isLoading) {
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
                        }.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Download my data", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Get a copy of your profile and history", color = c.muted, fontSize = 13.sp)
                    }
                }
                Text("Your Play account email is only shared with a developer after you join their test.", color = c.muted, fontSize = 13.sp, lineHeight = 18.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GroupLabel("Session")
                PillButton("Sign out", { signOutNow() }, Modifier.fillMaxWidth(), 50.dp, outlined = true, content = c.ink, enabled = !isLoading)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GroupLabel("Danger zone")
                AtcCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Deleting your account removes your profile, apps and testing history. Apps in an active test lose your check-ins.",
                            color = c.muted, fontSize = 14.sp, lineHeight = 20.sp
                        )
                        PillButton("Delete account", { confirmDelete = true }, Modifier.fillMaxWidth(), 50.dp, outlined = true, content = c.danger, enabled = !isLoading)
                    }
                }
            }
        }
    }
}