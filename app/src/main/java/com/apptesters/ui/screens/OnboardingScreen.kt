package com.apptesters.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.data.AppConfig
import com.apptesters.data.AuthRepository
import com.apptesters.domain.userMessage
import kotlinx.coroutines.launch

/**
 * Sign-in screen. After a successful sign-in the session status changes and
 * AppRoot moves on automatically, so onSignedIn is optional.
 */
@Composable
fun OnboardingScreen(onSignedIn: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "App Testers Community",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Test each other's apps and complete Google Play's 14-day closed testing together.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        val res = AuthRepository(context).signInWithGoogle(AppConfig.GOOGLE_WEB_CLIENT_ID)
                        isLoading = false
                        if (res.isSuccess) onSignedIn()
                        else {
                            val ex = res.exceptionOrNull()
                            errorMessage = (ex?.userMessage() ?: "Sign in failed") +
                                    "\n[${ex?.javaClass?.simpleName}: ${ex?.message}]"
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text("Sign in with Google")
            }
        }
    }
}
