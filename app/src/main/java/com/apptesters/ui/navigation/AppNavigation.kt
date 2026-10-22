package com.apptesters.ui.navigation

import androidx.compose.foundation.layout.Box
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
import com.apptesters.ui.screens.DiscoverScreen
import com.apptesters.ui.screens.HomeScreen
import com.apptesters.ui.screens.ProfileScreen

enum class AppDestination(val title: String) {
    HOME("Home"),
    DISCOVER("Discover"),
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
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentDestination) {
                AppDestination.HOME -> HomeScreen(onNavigateToDiscover = { currentDestination = AppDestination.DISCOVER })
                AppDestination.DISCOVER -> DiscoverScreen()
                AppDestination.PROFILE -> ProfileScreen()
            }
        }
    }
}
