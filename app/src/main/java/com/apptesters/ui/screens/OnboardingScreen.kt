package com.apptesters.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptesters.R
import com.apptesters.data.AppConfig
import com.apptesters.data.AuthRepository
import com.apptesters.domain.userMessage
import com.apptesters.ui.components.PillButton
import com.apptesters.ui.theme.Bricolage
import com.apptesters.ui.theme.Lime
import kotlinx.coroutines.launch

private val Ink = Color(0xFF121212)
private val Paper = Color(0xFFF3F1EA)

/** Onboarding / sign-in. Always dark, like the design. */
@Composable
fun OnboardingScreen(onSignedIn: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(painterResource(R.drawable.ic_logo), null, Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            Text("App Testers Community", color = Paper, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Text(
            buildAnnotatedString {
                append("12 testers.\n14 days.\n")
                withStyle(SpanStyle(color = Lime)) { append("Your app on Play.") }
            },
            color = Paper, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 50.sp
        )

        Text(
            "Test other developers' apps, and they test yours. Everyone's daily progress is shown live.",
            color = Color(0xFFCFCCC0), fontSize = 17.sp, lineHeight = 24.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                "Create a profile with your Play account email",
                "Check in on apps you test, once a day",
                "Add your app and watch testers arrive"
            ).forEachIndexed { i, t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).border(1.5.dp, Lime, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = Lime, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(t, color = Paper, fontSize = 16.sp, lineHeight = 21.sp)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            error?.let { Text(it, color = Color(0xFFF2B8B5), fontSize = 14.sp) }
            PillButton(
                text = if (loading) "Signing in…" else "Continue with Google",
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        loading = true; error = null
                        val res = AuthRepository(context).signInWithGoogle(AppConfig.GOOGLE_WEB_CLIENT_ID)
                        loading = false
                        if (res.isSuccess) onSignedIn() else error = res.exceptionOrNull()?.userMessage() ?: "Sign in failed"
                    }
                }
            )
            Text(
                "By continuing you agree to our Terms and Privacy Policy.",
                color = Color(0xFF8E8A7B), fontSize = 12.sp, modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
