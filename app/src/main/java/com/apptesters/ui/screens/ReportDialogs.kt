package com.apptesters.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Asks for a short written reason (1 to maxLen characters). */
@Composable
fun ReasonDialog(
    title: String,
    label: String,
    submitLabel: String = "Send",
    maxLen: Int = 500,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= maxLen) text = it },
                label = { Text(label) },
                supportingText = { Text("${text.length}/$maxLen") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(text.trim()) }, enabled = text.isNotBlank()) { Text(submitLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Tester feedback to the developer: bug, suggestion or praise (1 to 1000 characters). */
@Composable
fun FeedbackDialog(onDismiss: () -> Unit, onSubmit: (kind: String, body: String) -> Unit) {
    var kind by remember { mutableStateOf("bug") }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("bug" to "Bug", "suggestion" to "Idea", "praise" to "Praise").forEach { (k, l) ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(l) })
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 1000) text = it },
                    label = { Text("Your message") },
                    supportingText = { Text("${text.length}/1000") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(kind, text.trim()) }, enabled = text.isNotBlank()) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
