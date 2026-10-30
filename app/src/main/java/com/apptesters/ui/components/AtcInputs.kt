package com.apptesters.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.ui.theme.Atc
import com.apptesters.ui.theme.DmSans

/** Pill search field (Discover). */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = Atc.colors
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(c.surface).border(1.5.dp, c.line, CircleShape).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AtcIcon(AtcIcons.Search, c.muted, 20.dp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = c.muted, fontSize = 15.sp)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = c.ink, fontSize = 15.sp, fontFamily = DmSans),
                cursorBrush = SolidColor(c.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Filter chip: selected = filled ink (lime text) in light, lime in dark. */
@Composable
fun AtcChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Atc.colors
    val selBg = if (c.isDark) c.accent else c.ink
    val selFg = if (c.isDark) c.onAccent else c.accent
    Box(
        Modifier.height(44.dp).clip(CircleShape)
            .background(if (selected) selBg else if (c.isDark) c.surface else c.surface)
            .border(1.5.dp, if (selected) selBg else if (c.isDark) c.line else c.ink, CircleShape)
            .clickable { onClick() }.padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (selected) selFg else c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

/** Small outlined tag, e.g. category. */
@Composable
fun TagPill(text: String) {
    val c = Atc.colors
    Text(
        text, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        modifier = Modifier.border(1.5.dp, if (c.isDark) c.line else c.ink, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Round back button + optional trailing slot, used at the top of sub screens. */
@Composable
fun AtcTopBar(onBack: (() -> Unit)?, title: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = Atc.colors
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                AtcIcon(AtcIcons.Back, c.ink, 24.dp)
            }
        } else Spacer(Modifier.width(8.dp))
        if (title != null) Text(title, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** Labelled rounded text field (forms). */
@Composable
fun AtcTextField(
    label: String, value: String, onChange: (String) -> Unit, placeholder: String = "",
    helper: String? = null, error: Boolean = false, singleLine: Boolean = true, minLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    val c = Atc.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Box(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .background(c.surface)
                .border(1.5.dp, if (error) c.danger else if (c.isDark) c.line else c.dashed, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
        ) {
            if (value.isEmpty()) Text(placeholder, color = c.muted, fontSize = 16.sp)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = singleLine, minLines = minLines,
                textStyle = TextStyle(color = c.ink, fontSize = 16.sp, fontFamily = DmSans),
                cursorBrush = SolidColor(c.ink), modifier = Modifier.fillMaxWidth()
            )
        }
        if (helper != null) Text(helper, color = if (error) c.danger else c.muted, fontSize = 13.sp)
    }
}
