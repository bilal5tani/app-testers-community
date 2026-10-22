package com.apptesters.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.apptesters.ui.screens.*

enum class AppDestination(val title: String) {
    HOME("Home"),
    DISCOVER("Discover"),
    ADD_APP("Add App"),
    NOTIFICATIONS("Inbox"),
    PROFILE("Profile")
}

sealed interface SubScreen {
    data object None : SubScreen
    data class AppDetail(val appId: String) : SubScreen
    data class LiveProgress(val appId: String) : SubScreen
    data object Settings : SubScreen
    data object EditProfile : SubScreen
    data object NotificationSettings : SubScreen
    data object Appearance : SubScreen
    data object PrivacyAccount : SubScreen
}

@Composable
fun MainAppNavigation() {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var subScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val screen = subScreen) {
            is SubScreen.AppDetail -> {
                AppDetailScreen(
                    appId = screen.appId,
                    onBack = { subScreen = SubScreen.None },
                    onNavigateToLiveProgress = { id -> subScreen = SubScreen.LiveProgress(id) }
                )
            }
            is SubScreen.LiveProgress -> {
                LiveProgressScreen(
                    appId = screen.appId,
                    onBack = { subScreen = SubScreen.None }
                )
            }
            is SubScreen.Settings -> {
                SettingsScreen(
                    onNavigateToEditProfile = { subScreen = SubScreen.EditProfile },
                    onNavigateToNotificationSettings = { subScreen = SubScreen.NotificationSettings },
                    onNavigateToAppearance = { subScreen = SubScreen.Appearance },
                    onNavigateToPrivacy = { subScreen = SubScreen.PrivacyAccount },
                    onBack = { subScreen = SubScreen.None }
                )
            }
            is SubScreen.EditProfile -> {
                EditProfileScreen(onBack = { subScreen = SubScreen.Settings })
            }
            is SubScreen.NotificationSettings -> {
                NotificationSettingsScreen(onBack = { subScreen = SubScreen.Settings })
            }
            is SubScreen.Appearance -> {
                AppearanceScreen(onBack = { subScreen = SubScreen.Settings })
            }
            is SubScreen.PrivacyAccount -> {
                PrivacyAccountScreen(
                    onBack = { subScreen = SubScreen.Settings },
                    onSignedOut = { subScreen = SubScreen.None }
                )
            }
            SubScreen.None -> {
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
                            AppDestination.HOME -> HomeScreen(
                                onNavigateToDiscover = { currentDestination = AppDestination.DISCOVER },
                                onNavigateToAppDetail = { appId -> subScreen = SubScreen.AppDetail(appId) },
                                onNavigateToLiveProgress = {
                                    // If we have an active app ID, we could pass it or load it, let's pass a default or handle
                                    subScreen = SubScreen.LiveProgress("sample-app-1")
                                }
                            )
                            AppDestination.DISCOVER -> DiscoverScreen(
                                onNavigateToAppDetail = { appId -> subScreen = SubScreen.AppDetail(appId) }
                            )
                            AppDestination.ADD_APP -> AddAppScreen(
                                onAppCreated = { currentDestination = AppDestination.HOME }
                            )
                            AppDestination.NOTIFICATIONS -> NotificationsScreen()
                            AppDestination.PROFILE -> ProfileScreen(
                                onNavigateToSettings = { subScreen = SubScreen.Settings },
                                onNavigateToEditProfile = { subScreen = SubScreen.EditProfile }
                            )
                        }
                    }
                }
            }
        }
    }
}
