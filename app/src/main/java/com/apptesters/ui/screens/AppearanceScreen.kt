package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.repository.PreferencesRepository
import com.apptesters.ui.components.*
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.Bricolage
import com.apptesters.ui.theme.Lime
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(onBack: () -> Unit = {}) {
    val c = Atc.colors
    val context = LocalContext.current
    val repo = remember { PreferencesRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val theme by repo.themeFlow.collectAsState(initial = "system")
    val lang by repo.languageFlow.collectAsState(initial = "en")

    Column(Modifier.fillMaxSize().background(c.bg)) {
        AtcTopBar(onBack = onBack) {}
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            Text("Appearance", color = c.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(start = 4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GroupLabel("Theme")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (id, label) ->
                        val sel = theme == id
                        val preview = when (id) {
                            "light" -> Brush.linearGradient(listOf(Color(0xFFF3F1EA), Color(0xFFF3F1EA)))
                            "dark" -> Brush.linearGradient(listOf(Color(0xFF0F0F0E), Color(0xFF0F0F0E)))
                            else -> Brush.horizontalGradient(0.5f to Color(0xFFF3F1EA), 0.5f to Color(0xFF0F0F0E))
                        }
                        val bar1 = when (id) { "light" -> Color.White; "dark" -> Color(0xFF33332E); else -> Color(0xFF8E8A7B) }
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.surface)
                                .border(if (sel) 2.5.dp else 1.5.dp, if (sel) (if (c.isDark) c.accent else c.ink) else c.line, RoundedCornerShape(20.dp))
                                .clickable { scope.launch { repo.setTheme(id) } }.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).background(preview).padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Bottom)
                            ) {
                                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(bar1))
                                Box(Modifier.fillMaxWidth(0.6f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Lime))
                            }
                            Text(label, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Text("System follows your phone's light or dark setting.", color = c.muted, fontSize = 13.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GroupLabel("Language")
                AtcCard {
                    listOf(
                        Triple("en", "English", "Default"),
                        Triple("ur", "اردو", "Urdu · translation coming soon"),
                        Triple("rur", "Roman Urdu", "Urdu in English letters · coming soon")
                    ).forEachIndexed { i, (id, label, sub) ->
                        if (i > 0) HorizontalDivider(color = c.divider)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { scope.launch { repo.setLanguage(id) } }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(label, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(sub, color = c.muted, fontSize = 13.sp)
                            }
                            val edge = if (c.isDark) c.accent else c.ink
                            Box(Modifier.size(24.dp).border(2.dp, edge, CircleShape), contentAlignment = Alignment.Center) {
                                if (lang == id) Box(Modifier.size(12.dp).clip(CircleShape).background(edge))
                            }
                        }
                    }
                }
            }
        }
    }
}
