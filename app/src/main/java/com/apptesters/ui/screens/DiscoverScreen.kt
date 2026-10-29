package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.dto.DiscoverAppDto
import com.apptesters.data.repository.AppsRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CATEGORIES = listOf<Pair<String?, String>>(
    null to "All", "tools" to "Tools", "games" to "Games",
    "education" to "Education", "lifestyle" to "Lifestyle", "other" to "Other"
)
private const val PAGE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(onNavigateToAppDetail: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var apps by remember { mutableStateOf<List<DiscoverAppDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(query, category, reload) {
        if (query.isNotEmpty()) delay(350)
        loading = true
        error = null
        val r = AppsRepository().discover(query.trim().ifEmpty { null }, category, PAGE, 0)
        r.onSuccess { apps = it; endReached = it.size < PAGE }
        r.onFailure { error = it.userMessage() }
        loading = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Discover Apps", fontWeight = FontWeight.Bold) }) }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search apps...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(CATEGORIES) { (value, label) ->
                    FilterChip(
                        selected = category == value,
                        onClick = { category = value },
                        label = { Text(label) }
                    )
                }
            }

            when {
                loading -> LoadingBox()
                error != null -> ErrorBox(error!!, onRetry = { reload++ })
                apps.isEmpty() -> EmptyBox("No apps found. Try another search or category.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(apps, key = { it.id }) { app ->
                        Card(onClick = { onNavigateToAppDetail(app.id) }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(app.iconUrl, app.title)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${app.category.replaceFirstChar { it.uppercase() }} • " +
                                            if (app.spotsLeft > 0) "${app.spotsLeft} spots left" else "Full",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "By ${app.ownerName} • Karma ${app.ownerKarma}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    if (!app.description.isNullOrBlank()) {
                                        Text(app.description, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (app.myStatus == "joined") {
                                        Text("Testing ✓", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    if (!endReached) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        loadingMore = true
                                        val r = AppsRepository().discover(query.trim().ifEmpty { null }, category, PAGE, apps.size)
                                        r.onSuccess { apps = apps + it; endReached = it.size < PAGE }
                                        loadingMore = false
                                    }
                                },
                                enabled = !loadingMore,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (loadingMore) "Loading..." else "Load more") }
                        }
                    }
                }
            }
        }
    }
}
