package com.apptesters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.apptesters.push.Notifications
import com.apptesters.ui.navigation.AppRoot
import com.apptesters.ui.theme.AppTestersCommunityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.createChannels(this)
        enableEdgeToEdge()
        setContent {
            AppTestersCommunityTheme {
                AppRoot()
            }
        }
    }
}
