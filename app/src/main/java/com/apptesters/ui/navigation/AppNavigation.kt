package com.apptesters.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class AppDestination(val title: String) {
    HOME("Home"),
    DISCOVER("Discover"),
    LIVE_PROGRESS("Progress"),
    NOTIFICATIONS("Notifications"),
    PROFILE("Profile")
}

@Composable
fun MainAppNavigation() {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        label = { Text(destination.title) },
                        icon = {}
                    )
                }
            }
        }
    ) { innerPadding ->
        Text(
            text = "Active Screen: ${currentDestination.title}",
            modifier = Modifier.padding(innerPadding)
        )
    }
}
