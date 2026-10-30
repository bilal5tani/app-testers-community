package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.repository.AppsRepository
import com.apptesters.domain.userMessage
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import kotlinx.coroutines.launch

private val PACKAGE_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")
private val CATEGORY_OPTIONS = listOf("tools", "games", "education", "lifestyle", "other")

@Composable
fun AddAppScreen(onAppCreated: () -> Unit = {}) {
    val c = Atc.colors
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var optInUrl by remember { mutableStateOf("") }
    var groupUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("tools") }
    var testersNeeded by remember { mutableIntStateOf(12) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val pkg = packageName.trim()
    val optIn = optInUrl.trim()
    val group = groupUrl.trim()
    val pkgOk = pkg.isEmpty() || PACKAGE_REGEX.matches(pkg)
    val optOk = optIn.isEmpty() || optIn.startsWith("https://play.google.com/")
    val groupOk = group.isEmpty() || group.startsWith("https://groups.google.com/")
    val canSubmit = !loading && title.isNotBlank() && pkg.isNotEmpty() && pkgOk &&
        optIn.isNotEmpty() && optOk && group.isNotEmpty() && groupOk

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Text(
            "Add your app", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
            modifier = Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp)
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                "Share your closed-testing app so other developers can join. Testers check in daily and you watch the 14-day count live.",
                color = c.muted, fontSize = 14.sp, lineHeight = 20.sp
            )
            AtcTextField("App name", title, { title = it }, "e.g. Dastarkhwan")
            AtcTextField(
                "Package name", packageName, { packageName = it }, "com.yourname.app",
                helper = if (!pkgOk) "Use lowercase letters, digits and dots, like com.example.app" else null, error = !pkgOk
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CATEGORY_OPTIONS) { opt -> AtcChip(opt.replaceFirstChar { it.uppercase() }, category == opt) { category = opt } }
                }
            }
            AtcTextField(
                "Play Store opt-in link", optInUrl, { optInUrl = it }, "https://play.google.com/apps/testing/…",
                helper = if (!optOk) "Must start with https://play.google.com/" else "From Play Console > Testing > Closed testing",
                error = !optOk
            )
            AtcTextField(
                "Google Group link", groupUrl, { groupUrl = it }, "https://groups.google.com/g/…",
                helper = if (!groupOk) "Must start with https://groups.google.com/" else "Testers join this group so Google lets them install your app.",
                error = !groupOk
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Testers needed", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(12, 15, 20).forEach { n ->
                        val sel = testersNeeded == n
                        PillButton(
                            "$n testers", { testersNeeded = n }, Modifier.weight(1f), 48.dp,
                            container = if (sel) c.accent else c.surface, content = if (sel) c.onAccent else c.ink,
                            outlined = !sel
                        )
                    }
                }
                Text("Google asks for at least 12 testers opted in for 14 days in a row.", color = c.muted, fontSize = 13.sp)
            }
            AtcTextField(
                "What should testers look at?", description, { description = it },
                "Short note on what to try and what feedback you want", singleLine = false, minLines = 3
            )
            error?.let { Text(it, color = c.danger, fontSize = 14.sp) }
        }

        Column(
            Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillButton(
                if (loading) "Adding…" else "Add to the testing queue",
                onClick = {
                    scope.launch {
                        loading = true; error = null
                        val r = AppsRepository().createApp(
                            title = title.trim(), packageName = pkg, playStoreUrl = optIn, googleGroupUrl = group,
                            category = category, description = description.trim().ifEmpty { null }, testersNeeded = testersNeeded
                        )
                        loading = false
                        if (r.isSuccess) onAppCreated() else error = r.exceptionOrNull()?.userMessage() ?: "Failed to create app."
                    }
                },
                modifier = Modifier.fillMaxWidth(), height = 54.dp,
                container = if (c.isDark) c.accent else c.ink, content = if (c.isDark) c.onAccent else c.accent,
                enabled = canSubmit
            )
            Text("Your app also joins the queue for you to test others' apps in return.", color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
