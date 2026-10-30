package com.apptesters.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import com.apptesters.ui.theme.*

// ---------- Icons (24x24 stroke icons from the design) ----------
private fun strokeIcon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block
        )
    }.build()

object AtcIcons {
    val Home = strokeIcon("home") {
        moveTo(3f, 11f); lineTo(12f, 3f); lineTo(21f, 11f); verticalLineTo(20f)
        arcTo(1f, 1f, 0f, false, true, 20f, 21f); horizontalLineTo(15f); verticalLineTo(15f); horizontalLineTo(9f)
        verticalLineTo(21f); horizontalLineTo(4f); arcTo(1f, 1f, 0f, false, true, 3f, 20f); close()
    }
    val Compass = strokeIcon("compass") {
        moveTo(21f, 12f); arcTo(9f, 9f, 0f, true, true, 3f, 12f); arcTo(9f, 9f, 0f, true, true, 21f, 12f); close()
        moveTo(15.5f, 8.5f); lineTo(13.5f, 13.5f); lineTo(8.5f, 15.5f); lineTo(10.5f, 10.5f); close()
    }
    val Plus = strokeIcon("plus") { moveTo(12f, 5f); verticalLineTo(19f); moveTo(5f, 12f); horizontalLineTo(19f) }
    val Person = strokeIcon("person") {
        moveTo(16f, 8f); arcTo(4f, 4f, 0f, true, true, 8f, 8f); arcTo(4f, 4f, 0f, true, true, 16f, 8f); close()
        moveTo(4f, 21f); curveTo(4f, 17f, 8f, 15f, 12f, 15f); curveTo(16f, 15f, 20f, 17f, 20f, 21f)
    }
    val Bell = strokeIcon("bell") {
        moveTo(6f, 8f); arcTo(6f, 6f, 0f, false, true, 18f, 8f); curveTo(18f, 15f, 21f, 16f, 21f, 16f)
        horizontalLineTo(3f); curveTo(3f, 16f, 6f, 15f, 6f, 8f)
        moveTo(10.3f, 21f); arcTo(1.9f, 1.9f, 0f, false, false, 13.7f, 21f)
    }
    val Arrow = strokeIcon("arrow") {
        moveTo(5f, 12f); horizontalLineTo(19f); moveTo(13f, 6f); lineTo(19f, 12f); lineTo(13f, 18f)
    }
    val Chevron = strokeIcon("chevron") { moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f) }
    val Back = strokeIcon("back") { moveTo(15f, 6f); lineTo(9f, 12f); lineTo(15f, 18f) }
    val Search = strokeIcon("search") {
        moveTo(18f, 11f); arcTo(7f, 7f, 0f, true, true, 4f, 11f); arcTo(7f, 7f, 0f, true, true, 18f, 11f); close()
        moveTo(20f, 20f); lineTo(16f, 16f)
    }
    val More = strokeIcon("more") {
        moveTo(12f, 5f); lineTo(12f, 5.01f); moveTo(12f, 12f); lineTo(12f, 12.01f); moveTo(12f, 19f); lineTo(12f, 19.01f)
    }
    val Warn = strokeIcon("warn") { moveTo(12f, 7f); verticalLineTo(13f); moveTo(12f, 17f); lineTo(12f, 17.01f) }
    val Sliders = strokeIcon("sliders") {
        moveTo(4f, 6f); horizontalLineTo(12f); moveTo(16f, 6f); horizontalLineTo(20f)
        moveTo(4f, 12f); horizontalLineTo(6f); moveTo(10f, 12f); horizontalLineTo(20f)
        moveTo(4f, 18f); horizontalLineTo(14f); moveTo(18f, 18f); horizontalLineTo(20f)
        moveTo(14f, 6f); lineTo(14f, 6.01f); moveTo(8f, 12f); lineTo(8f, 12.01f); moveTo(16f, 18f); lineTo(16f, 18.01f)
    }
    val User = strokeIcon("user") {
        moveTo(4f, 21f); curveTo(4f, 17f, 8f, 15f, 12f, 15f); curveTo(16f, 15f, 20f, 17f, 20f, 21f)
        moveTo(16f, 8f); arcTo(4f, 4f, 0f, true, true, 8f, 8f); arcTo(4f, 4f, 0f, true, true, 16f, 8f); close()
    }
    val Lock = strokeIcon("lock") {
        moveTo(6f, 11f); horizontalLineTo(18f); verticalLineTo(20f); horizontalLineTo(6f); close()
        moveTo(8f, 11f); verticalLineTo(8f); arcTo(4f, 4f, 0f, false, true, 16f, 8f); verticalLineTo(11f)
    }
    val Sun = strokeIcon("sun") {
        moveTo(16f, 12f); arcTo(4f, 4f, 0f, true, true, 8f, 12f); arcTo(4f, 4f, 0f, true, true, 16f, 12f); close()
        moveTo(12f, 3f); verticalLineTo(5f); moveTo(12f, 19f); verticalLineTo(21f); moveTo(3f, 12f); horizontalLineTo(5f); moveTo(19f, 12f); horizontalLineTo(21f)
    }
    val Check = strokeIcon("check") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(19f, 7f) }
}

@Composable
fun AtcIcon(icon: ImageVector, tint: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

// ---------- Text helpers ----------
@Composable
fun SectionHeader(title: String, trailing: String? = null, onTrailingClick: (() -> Unit)? = null) {
    val c = Atc.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = c.ink)
        if (trailing != null) {
            val m = if (onTrailingClick != null) Modifier.clickable { onTrailingClick() }.padding(vertical = 10.dp) else Modifier
            Text(
                trailing, modifier = m, color = if (onTrailingClick != null) c.ink else c.muted,
                fontSize = if (onTrailingClick != null) 14.sp else 13.sp,
                fontWeight = if (onTrailingClick != null) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ---------- Cards / avatars ----------
@Composable
fun AtcCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Atc.colors
    val base = modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface)
    Column(if (onClick != null) base.clickable { onClick() } else base, content = content)
}

fun tintFor(key: String): Color = AvatarTints[(key.hashCode().and(0x7fffffff)) % AvatarTints.size]

@Composable
fun LetterAvatar(name: String, size: Dp = 44.dp, corner: Dp = 13.dp, fontSize: Int = 18) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(corner)).background(tintFor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase(), color = Color(0xFF121212),
            fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = fontSize.sp
        )
    }
}

@Composable
fun InitialsCircle(name: String, size: Dp = 44.dp) {
    val c = Atc.colors
    val initials = name.trim().split(" ", ".", "_").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifEmpty { "?" }
    Box(Modifier.size(size).clip(CircleShape).background(if (c.isDark) c.accent else Color(0xFF121212)), contentAlignment = Alignment.Center) {
        Text(initials, color = if (c.isDark) Color(0xFF121212) else Lime, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.36f).sp)
    }
}

// ---------- Buttons ----------
@Composable
fun PillButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 56.dp,
    container: Color = Atc.colors.accent, content: Color = Atc.colors.onAccent,
    enabled: Boolean = true, outlined: Boolean = false, trailingIcon: ImageVector? = null,
) {
    val c = Atc.colors
    val bg = if (outlined) Color.Transparent else if (enabled) container else c.track
    val fg = if (enabled) content else c.muted
    Row(
        modifier.height(height).clip(CircleShape).background(bg)
            .then(if (outlined) Modifier.border(BorderStroke(1.5.dp, if (enabled) content else c.line), CircleShape) else Modifier)
            .clickable(enabled = enabled) { onClick() }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
        if (trailingIcon != null) { Spacer(Modifier.width(8.dp)); AtcIcon(trailingIcon, fg, 18.dp) }
    }
}

@Composable
fun CircleIconButton(icon: ImageVector, onClick: () -> Unit, badge: Boolean = false) {
    val c = Atc.colors
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (c.isDark) c.surfaceAlt else c.surface)
                .border(1.5.dp, c.line, CircleShape).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) { AtcIcon(icon, c.ink) }
        if (badge) Box(Modifier.size(11.dp).clip(CircleShape).background(Lime).border(2.dp, c.bg, CircleShape))
    }
}

@Composable
fun DashedButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    val c = Atc.colors
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(22.dp)).clickable { onClick() }
            .drawBehind {
                drawRoundRect(
                    c.dashed, cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                )
            },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) { AtcIcon(icon, c.ink, 20.dp); Spacer(Modifier.width(8.dp)) }
        Text(text, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// ---------- Progress ----------
/** N-segment bar (14 days). */
@Composable
fun SegmentBar(done: Int, total: Int = 14, on: Color, off: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total) { i ->
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(if (i < done) on else off))
        }
    }
}

@Composable
fun ThinBar(fraction: Float, modifier: Modifier = Modifier) {
    val c = Atc.colors
    val fill = if (c.isDark) c.accent else Color(0xFF121212)
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(3.dp)).background(fill))
    }
}

// ---------- Bottom nav ----------
data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun AtcBottomBar(items: List<NavItem>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val c = Atc.colors
    Column(Modifier.background(c.navBg).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.navBorder))
        Row(Modifier.fillMaxWidth().height(72.dp)) {
            items.forEachIndexed { i, item ->
                val sel = i == selectedIndex
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable(
                        indication = null,
                        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.width(56.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (sel) c.navPillBg else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) { AtcIcon(item.icon, if (sel) c.navPillIcon else c.muted) }
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) c.ink else c.muted)
                }
            }
        }
    }
}
