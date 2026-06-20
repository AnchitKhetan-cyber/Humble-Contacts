package com.humblesolutions.humblecontacts

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.humblecontacts.navigation.AppNavGraph
import com.humblesolutions.humblecontacts.navigation.Routes
import com.humblesolutions.humblecontacts.ui.theme.HumbleContactsTheme

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkMode by remember { mutableStateOf(systemDark) }

            LaunchedEffect(systemDark) { darkMode = systemDark }

            HumbleContactsTheme(darkTheme = darkMode) {
                // Parse contact ID from deep link if app was opened via URL
                val deepLinkContactId = intent?.data
                    ?.takeIf { it.host == "humblecontacts.page.link" }
                    ?.lastPathSegment

                val startDestination = if (deepLinkContactId != null)
                    Routes.contactDetail(deepLinkContactId)
                else
                    Routes.SPLASH

                AppNavGraph(
                    startDestination = startDestination,
                    darkMode = darkMode,
                    onDarkModeChange = { darkMode = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}