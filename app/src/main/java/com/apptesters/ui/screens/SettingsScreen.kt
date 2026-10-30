package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.AuthRepository
import com.apptesters.data.repository.ProfileRepository
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val c = Atc.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    LaunchedEffect(Unit) {
        ProfileRepository().getProfile().getOrNull()?.let { name = it.profile.displayName; username = it.profile.username ?: "" }
    }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        AtcTopBar(onBack = onBack, title = null) {}
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Text("Settings", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(start = 4.dp))

            AtcCard(onClick = onNavigateToEditProfile) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialsCircle(name.ifBlank { "?" }, 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name.ifBlank { "Your profile" }, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
                        Text("@$username · Edit profile", color = c.muted, fontSize = 13.sp, maxLines = 1)
                    }
                    AtcIcon(AtcIcons.Chevron, c.muted, 20.dp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GroupLabel("Account")
                AtcCard {
                    NavRow(AtcIcons.User, "Edit profile", "Name, bio, Play account email", onNavigateToEditProfile)
                    HorizontalDivider(color = c.divider)
                    NavRow(AtcIcons.Lock, "Privacy & account", "Visibility, data, delete account", onNavigateToPrivacy)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GroupLabel("Preferences")
                AtcCard {
                    NavRow(AtcIcons.Bell, "Notifications", "Reminders and alerts", onNavigateToNotificationSettings)
                    HorizontalDivider(color = c.divider)
                    NavRow(AtcIcons.Sun, "Appearance", "Theme and language", onNavigateToAppearance)
                }
            }

            PillButton(
                "Log out", { scope.launch { runCatching { AuthRepository(context).signOut() } } },
                Modifier.fillMaxWidth(), 52.dp, outlined = true, content = c.ink
            )
            Text("App Testers Community · v$version", color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
