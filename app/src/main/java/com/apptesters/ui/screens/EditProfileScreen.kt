package com.apptesters.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
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

    val c = Atc.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        if (firstRun) Spacer(Modifier.statusBarsPadding().height(16.dp)) else AtcTopBar(onBack = onBack) {}
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                if (firstRun) "Set up your profile" else "Edit profile", color = c.ink,
                fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
                modifier = Modifier.padding(start = if (firstRun) 0.dp else 4.dp)
            )
            if (firstRun) Text("Choose a username so other testers can find you.", color = c.muted, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsCircle(displayName.ifBlank { "?" }, 80.dp)
            }
            AtcTextField("Display name", displayName, { if (it.length <= 40) displayName = it }, "Your name")
            AtcTextField(
                "Username", username,
                { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' }.take(20) },
                "username",
                helper = if (username.isNotEmpty() && !usernameValid) "3-20 characters: a-z, 0-9, dot, underscore" else "Shown to other testers and developers.",
                error = username.isNotEmpty() && !usernameValid
            )
            AtcTextField(
                "Play account email", playEmail, { playEmail = it }, "your Gmail address",
                helper = "Used to add you to Google Groups for closed tests. Only shared when you join a test."
            )
            AtcTextField("Test device", deviceModel, { deviceModel = it }, "your phone model")
            AtcTextField("Android version", androidVersion, { androidVersion = it }, "e.g. 14")
            AtcTextField("Bio", bio, { if (it.length <= 160) bio = it }, "What do you build?", helper = "${bio.length}/160", singleLine = false, minLines = 3)
            errorMessage?.let { Text(it, color = c.danger, fontSize = 14.sp) }
        }
        Column(Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)) {
            PillButton(
                if (isLoading) "Saving…" else if (firstRun) "Continue" else "Save changes",
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
                        if (res.isSuccess) onSaved() else errorMessage = res.exceptionOrNull()?.userMessage() ?: "Update failed"
                    }
                },
                modifier = Modifier.fillMaxWidth(), height = 54.dp,
                container = if (c.isDark) c.accent else c.ink, content = if (c.isDark) c.onAccent else c.accent,
                enabled = canSave
            )
        }
    }
}
