package com.apptesters.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.apptesters.data.repository.PreferencesRepository

val LocalAtc = staticCompositionLocalOf { LightAtcColors }

/** Shortcut: Atc.colors.surface etc. */
object Atc {
    val colors: AtcColors @Composable get() = LocalAtc.current
}

@Composable
fun AppTestersCommunityTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val repo = remember { PreferencesRepository(context.applicationContext) }
    val pref by repo.themeFlow.collectAsState(initial = "system")
    val dark = when (pref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkAtcColors else LightAtcColors
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.surfaceAlt, onPrimaryContainer = c.ink,
        secondary = c.muted, background = c.bg, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
        surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.muted, outline = c.line, error = c.danger,
    ) else lightColorScheme(
        primary = c.ink, onPrimary = c.accent, primaryContainer = c.surfaceAlt, onPrimaryContainer = c.ink,
        secondary = c.muted, background = c.bg, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
        surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.muted, outline = c.line, error = c.danger,
    )
    CompositionLocalProvider(LocalAtc provides c) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
