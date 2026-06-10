package com.humblesolutions.humblecontacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.humblecontacts.navigation.AppNavGraph
import com.humblesolutions.humblecontacts.navigation.Routes
import com.humblesolutions.humblecontacts.ui.theme.HumbleContactsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkMode by remember { mutableStateOf(systemDark) }

            LaunchedEffect(systemDark) {
                darkMode = systemDark
            }

            HumbleContactsTheme(darkTheme = darkMode) {
                AppNavGraph(
                    startDestination = Routes.SPLASH,
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