package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apptesters.data.repository.AppsRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.launch

private val PACKAGE_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")
private val CATEGORY_OPTIONS = listOf("tools", "games", "education", "lifestyle", "other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppScreen(onAppCreated: () -> Unit = {}) {
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("List Your App", fontWeight = FontWeight.Bold) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("App name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = packageName, onValueChange = { packageName = it },
                label = { Text("Package name (com.example.app)") },
                isError = !pkgOk,
                supportingText = { if (!pkgOk) Text("Use lowercase letters, digits and dots, like com.example.app") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = optInUrl, onValueChange = { optInUrl = it },
                label = { Text("Play Store opt-in link") },
                isError = !optOk,
                supportingText = { Text(if (!optOk) "Must start with https://play.google.com/" else "From Play Console > Testing > Closed testing") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = groupUrl, onValueChange = { groupUrl = it },
                label = { Text("Google Group link") },
                isError = !groupOk,
                supportingText = { Text(if (!groupOk) "Must start with https://groups.google.com/" else "The group you use as the tester list") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth()
            )

            Text("Category", fontWeight = FontWeight.Medium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CATEGORY_OPTIONS) { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Text("Testers needed", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12, 15, 20).forEach { n ->
                    FilterChip(selected = testersNeeded == n, onClick = { testersNeeded = n }, label = { Text("$n") })
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        val r = AppsRepository().createApp(
                            title = title.trim(),
                            packageName = pkg,
                            playStoreUrl = optIn,
                            googleGroupUrl = group,
                            category = category,
                            description = description.trim().ifEmpty { null },
                            testersNeeded = testersNeeded
                        )
                        loading = false
                        if (r.isSuccess) onAppCreated()
                        else error = r.exceptionOrNull()?.userMessage() ?: "Failed to create app."
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(24.dp)) else Text("Submit App")
            }
        }
    }
}
