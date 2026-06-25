package com.humblesolutions.humblecontacts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.humblesolutions.humblecontacts.navigation.AppNavGraph
import com.humblesolutions.humblecontacts.navigation.Routes
import com.humblesolutions.humblecontacts.notifications.NotificationHelper
import com.humblesolutions.humblecontacts.ui.theme.HumbleContactsTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled silently */ }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels (safe to call multiple times)
        NotificationHelper.createChannels(this)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkMode by remember { mutableStateOf(systemDark) }

            LaunchedEffect(systemDark) { darkMode = systemDark }

            HumbleContactsTheme(darkTheme = darkMode) {
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

    override fun onResume() {
        super.onResume()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
