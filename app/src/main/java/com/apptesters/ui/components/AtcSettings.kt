package com.apptesters.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.ui.theme.Atc

/** Small uppercase group label ("ACCOUNT"). */
@Composable
fun GroupLabel(text: String) {
    Text(text.uppercase(), color = Atc.colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.8.sp)
}

/** Design toggle: 52x32 track with a 22dp knob. */
@Composable
fun AtcToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Atc.colors
    val track = if (checked) (if (c.isDark) c.accent else c.ink) else c.track
    val edge = if (checked) track else c.dashed
    val knob = if (checked) (if (c.isDark) c.onAccent else c.accent) else c.muted
    Box(
        Modifier.size(width = 60.dp, height = 48.dp).clickable(role = Role.Switch) { onChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(width = 52.dp, height = 32.dp).clip(CircleShape).background(track).border(1.5.dp, edge, CircleShape)) {
            Box(
                Modifier.padding(start = if (checked) 22.dp else 3.dp, top = 3.dp).size(22.dp)
                    .clip(CircleShape).background(knob)
            )
        }
    }
}

@Composable
fun ToggleRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Atc.colors
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (sub != null) Text(sub, color = c.muted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        AtcToggle(checked, onChange)
    }
}

/** Settings list row with a round icon tile and chevron. */
@Composable
fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sub: String, onClick: () -> Unit) {
    val c = Atc.colors
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(c.bg), contentAlignment = Alignment.Center) {
            AtcIcon(icon, c.ink, 20.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(sub, color = c.muted, fontSize = 13.sp)
        }
        AtcIcon(AtcIcons.Chevron, c.muted, 20.dp)
    }
}
