package com.apptesters

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.apptesters.ui.navigation.MainAppNavigation
import com.apptesters.ui.theme.AppTestersCommunityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "SUPABASE_URL: ${BuildConfig.SUPABASE_URL}")
        enableEdgeToEdge()
        setContent {
            AppTestersCommunityTheme {
                MainAppNavigation()
            }
        }
    }
}
