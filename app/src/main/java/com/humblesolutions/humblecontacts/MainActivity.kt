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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.humblesolutions.humblecontacts.navigation.AppNavGraph
import com.humblesolutions.humblecontacts.navigation.Routes
import com.humblesolutions.humblecontacts.notifications.NotificationHelper
import com.humblesolutions.humblecontacts.ui.theme.HumbleContactsTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled silently */ }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate — hands the system splash off to the app.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the system splash on screen until Compose has drawn the first frame,
        // so no window background can flash between the OS splash and the app splash.
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        enableEdgeToEdge()

        // Create notification channels (safe to call multiple times)
        NotificationHelper.createChannels(this)

        val themePreference = ThemePreference(this)

        setContent {
            LaunchedEffect(Unit) { keepSplashOnScreen = false }

            val systemDark = isSystemInDarkTheme()

            // null  = follow the device theme (updates live when the phone switches).
            // true/false = user override set via the Profile dark-mode toggle.
            var themeOverride by remember {
                mutableStateOf(
                    if (themePreference.hasDarkModeSet())
                        themePreference.isDarkMode()
                    else
                        null
                )
            }

            val darkMode = themeOverride ?: systemDark

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
                    onDarkModeChange = { turnedOn ->
                        // Any manual toggle (on or off) persists as an override that
                        // wins over the device theme.
                        themeOverride = turnedOn
                        themePreference.saveDarkMode(turnedOn)
                    }
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
