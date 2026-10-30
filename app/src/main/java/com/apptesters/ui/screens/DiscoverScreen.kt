package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
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
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CATEGORIES = listOf<Pair<String?, String>>(
    null to "All", "tools" to "Tools", "games" to "Games",
    "education" to "Education", "lifestyle" to "Lifestyle", "other" to "Other"
)
private const val PAGE = 20

@Composable
fun DiscoverScreen(onNavigateToAppDetail: (String) -> Unit = {}) {
    val c = Atc.colors
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

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Discover", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 34.sp)
            SearchField(query, { query = it }, "Search apps or developers")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CATEGORIES) { (value, label) -> AtcChip(label, category == value) { category = value } }
            }
        }

        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error!!, onRetry = { reload++ })
            apps.isEmpty() -> EmptyBox("No apps found. Try another search or category.")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("${apps.size} apps waiting for testers", color = c.muted, fontSize = 13.sp) }
                items(apps, key = { it.id }) { app ->
                    val joined = (app.testersNeeded - app.spotsLeft).coerceAtLeast(0)
                    AtcCard(onClick = { onNavigateToAppDetail(app.id) }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(app.iconUrl, app.title, 52.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("@${app.ownerName} · Karma ${app.ownerKarma}", color = c.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(8.dp))
                                TagPill(app.category.replaceFirstChar { it.uppercase() })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("$joined of ${app.testersNeeded} testers", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(if (app.spotsLeft > 0) "${app.spotsLeft} spots left" else "Full", color = c.muted, fontSize = 13.sp)
                                    }
                                    ThinBar(joined / app.testersNeeded.coerceAtLeast(1).toFloat())
                                }
                                Spacer(Modifier.width(12.dp))
                                val testing = app.myStatus == "joined"
                                Box(
                                    Modifier.height(44.dp).background(if (testing) c.track else c.accent, androidx.compose.foundation.shape.CircleShape).padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(if (testing) "Testing ✓" else "Test", color = if (testing) c.ink else c.onAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                            }
                        }
                    }
                }
                if (!endReached) {
                    item {
                        PillButton(
                            if (loadingMore) "Loading..." else "Load more",
                            onClick = {
                                scope.launch {
                                    loadingMore = true
                                    val r = AppsRepository().discover(query.trim().ifEmpty { null }, category, PAGE, apps.size)
                                    r.onSuccess { apps = apps + it; endReached = it.size < PAGE }
                                    loadingMore = false
                                }
                            },
                            enabled = !loadingMore, outlined = true, content = c.ink, height = 48.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
