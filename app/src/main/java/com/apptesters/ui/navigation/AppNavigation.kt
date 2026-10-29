package com.apptesters.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apptesters.data.AuthRepository
import com.apptesters.data.SupabaseClientProvider
import com.apptesters.data.repository.ProfileRepository
import com.apptesters.domain.userMessage
import com.apptesters.push.PushSetup
import com.apptesters.ui.screens.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

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

/** Top level: signed out -> Onboarding, signed in -> profile check -> main app. */
@Composable
fun AppRoot() {
    val status by SupabaseClientProvider.client.auth.sessionStatus.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (status) {
            is SessionStatus.Initializing -> CenteredLoader()
            is SessionStatus.Authenticated -> ProfileGate()
            else -> OnboardingScreen()
        }
    }
}

private sealed interface GateState {
    data object Loading : GateState
    data object NeedsSetup : GateState
    data object Ready : GateState
    data class Failed(val message: String) : GateState
}

@Composable
private fun ProfileGate() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<GateState>(GateState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        state = GateState.Loading
        val result = ProfileRepository().getProfile()
        state = if (result.isSuccess) {
            val username = result.getOrNull()?.profile?.username
            if (username.isNullOrBlank()) GateState.NeedsSetup else GateState.Ready
        } else {
            GateState.Failed(result.exceptionOrNull()?.userMessage() ?: "Something went wrong.")
        }
    }

    when (val s = state) {
        GateState.Loading -> CenteredLoader()
        GateState.NeedsSetup -> EditProfileScreen(
            firstRun = true,
            onSaved = { state = GateState.Ready }
        )
        GateState.Ready -> MainAppNavigation()
        is GateState.Failed -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(s.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            Button(onClick = { attempt++ }, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
            TextButton(onClick = { scope.launch { AuthRepository(context).signOut() } }) { Text("Sign out") }
        }
    }
}

@Composable
private fun CenteredLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MainAppNavigation() {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var subScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }

    PushSetup()

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
                EditProfileScreen(
                    onBack = { subScreen = SubScreen.Settings },
                    onSaved = { subScreen = SubScreen.Settings }
                )
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
                    onSignedOut = { /* AppRoot switches to Onboarding by itself */ }
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
                                onNavigateToLiveProgress = { appId -> subScreen = SubScreen.LiveProgress(appId) }
                            )
                            AppDestination.DISCOVER -> DiscoverScreen(
                                onNavigateToAppDetail = { appId -> subScreen = SubScreen.AppDetail(appId) }
                            )
                            AppDestination.ADD_APP -> AddAppScreen(
                                onAppCreated = { currentDestination = AppDestination.HOME }
                            )
                            AppDestination.NOTIFICATIONS -> NotificationsScreen(
                                onOpenApp = { appId -> subScreen = SubScreen.AppDetail(appId) }
                            )
                            AppDestination.PROFILE -> ProfileScreen(
                                onNavigateToSettings = { subScreen = SubScreen.Settings },
                                onNavigateToEditProfile = { subScreen = SubScreen.EditProfile },
                                onNavigateToAppDetail = { appId -> subScreen = SubScreen.AppDetail(appId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
