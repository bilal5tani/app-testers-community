package com.apptesters.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Design tokens from the UI/UX design (light + dark). */
@Immutable
data class AtcColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val divider: Color,
    val track: Color,
    val dashed: Color,
    val accent: Color,
    val onAccent: Color,
    val heroBg: Color,
    val heroInk: Color,
    val heroMuted: Color,
    val heroAccent: Color,
    val heroSegOn: Color,
    val heroSegOff: Color,
    val navBg: Color,
    val navBorder: Color,
    val navPillBg: Color,
    val navPillIcon: Color,
    val danger: Color,
)

val Lime = Color(0xFFCDF54A)

val LightAtcColors = AtcColors(
    isDark = false,
    bg = Color(0xFFF3F1EA), surface = Color(0xFFFFFFFF), surfaceAlt = Color(0xFFE6E3D8),
    ink = Color(0xFF121212), muted = Color(0xFF5C5A52),
    line = Color(0xFFCFCBBD), divider = Color(0xFFECE9DF), track = Color(0xFFE6E3D8), dashed = Color(0xFF8E8A7B),
    accent = Lime, onAccent = Color(0xFF121212),
    heroBg = Color(0xFF121212), heroInk = Color(0xFFF3F1EA), heroMuted = Color(0xFFB9B6AA),
    heroAccent = Lime, heroSegOn = Lime, heroSegOff = Color(0xFF3A3A36),
    navBg = Color(0xFFFFFFFF), navBorder = Color(0xFFDAD7CB),
    navPillBg = Color(0xFF121212), navPillIcon = Lime,
    danger = Color(0xFFB3261E),
)

val DarkAtcColors = AtcColors(
    isDark = true,
    bg = Color(0xFF0F0F0E), surface = Color(0xFF1D1D1A), surfaceAlt = Color(0xFF2A2A25),
    ink = Color(0xFFF3F1EA), muted = Color(0xFFA9A699),
    line = Color(0xFF3A3A35), divider = Color(0xFF2E2E2A), track = Color(0xFF33332E), dashed = Color(0xFF6B685D),
    accent = Lime, onAccent = Color(0xFF121212),
    heroBg = Lime, heroInk = Color(0xFF121212), heroMuted = Color(0xFF3B4416),
    heroAccent = Color(0xFF121212), heroSegOn = Color(0xFF121212), heroSegOff = Color(0x33121212),
    navBg = Color(0xFF1D1D1A), navBorder = Color(0xFF2E2E2A),
    navPillBg = Lime, navPillIcon = Color(0xFF121212),
    danger = Color(0xFFF2B8B5),
)

/** Pastel tints used behind letter avatars (same in light and dark; text is always dark). */
val AvatarTints = listOf(
    Color(0xFFDCD6FF), Color(0xFFFFD9B8), Color(0xFFBFE8DA),
    Color(0xFFFFE9A8), Color(0xFFC9DCFF), Color(0xFFF7C9D9),
)
