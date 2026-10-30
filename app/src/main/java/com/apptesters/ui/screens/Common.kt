package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.apptesters.ui.components.LetterAvatar
import com.apptesters.ui.components.PillButton
import com.apptesters.ui.theme.Atc

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Atc.colors.ink, strokeWidth = 3.dp)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, textAlign = TextAlign.Center, color = Atc.colors.danger)
        Spacer(Modifier.height(16.dp))
        PillButton("Retry", onRetry, height = 48.dp)
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = Atc.colors.muted)
    }
}

/** App icon from a URL, or a pastel letter tile when there is none. */
@Composable
fun AppIcon(url: String?, name: String, size: Dp = 48.dp) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url, contentDescription = name, contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.3f))
        )
    } else {
        LetterAvatar(name, size, size * 0.3f, (size.value * 0.42f).toInt())
    }
}

fun shortTime(iso: String): String = iso.take(16).replace('T', ' ')
