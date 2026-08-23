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
import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.humblecontacts.data.auth.PendingDeletionStore
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.humblesolutions.humblecontacts.navigation.AppNavGraph
import com.humblesolutions.humblecontacts.navigation.Routes
import com.humblesolutions.humblecontacts.notifications.NotificationHelper
import com.humblesolutions.humblecontacts.ui.components.OfflineBanner
import com.humblesolutions.humblecontacts.ui.theme.HumbleContactsTheme

class MainActivity : ComponentActivity() {

    companion object {
        /**
         * Host for contact-sharing https App Links (`https://<host>/contact/{id}`).
         * Must stay in sync with the App Links intent-filter host in AndroidManifest.xml
         * and with the domain that serves /.well-known/assetlinks.json.
         * See docs/applink-contact-domain-setup.md.
         */
        const val CONTACT_LINK_HOST = "links.humblecontacts.app"
    }

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

            // On first launch, adopt the device theme and persist it so the toggle is
            // saved from the start (dark device → saved dark, light device → saved light).
            // After that the saved value wins, and manual toggles persist over it.
            var darkMode by remember {
                mutableStateOf(
                    if (themePreference.hasDarkModeSet())
                        themePreference.isDarkMode()
                    else
                        systemDark
                )
            }

            LaunchedEffect(Unit) {
                if (!themePreference.hasDarkModeSet()) {
                    themePreference.saveDarkMode(systemDark)
                }
            }

            HumbleContactsTheme(darkTheme = darkMode) {
                val linkData = intent?.data?.takeIf { it.host == CONTACT_LINK_HOST }

                // Contact-save App Link (https://<host>/add?vcard=…): a card QR
                // scanned by any camera/QR app opens here → prefilled Add Contact.
                val deepLinkVCard = linkData
                    ?.takeIf { it.path?.startsWith("/add") == true }
                    ?.getQueryParameter("vcard")

                val deepLinkContactId = linkData
                    ?.takeIf { it.path?.startsWith("/contact/") == true }
                    ?.lastPathSegment

                // Returning account-deletion confirmation link: stash the URL for
                // DeleteAccountScreen to consume and route straight there (only if a
                // user is signed in — deletion requires it).
                val deletionEmailLink = intent?.data?.toString()
                    ?.takeIf { FirebaseAuth.getInstance().isSignInWithEmailLink(it) }
                val isDeletionLink =
                    deletionEmailLink != null && FirebaseAuth.getInstance().currentUser != null
                if (isDeletionLink) {
                    PendingDeletionStore(this@MainActivity).emailLink = deletionEmailLink
                }

                val startDestination = when {
                    isDeletionLink -> Routes.DELETE_ACCOUNT
                    !deepLinkVCard.isNullOrBlank() -> Routes.addContactWithVCard(deepLinkVCard)
                    deepLinkContactId != null -> Routes.contactDetail(deepLinkContactId)
                    else -> Routes.SPLASH
                }

                // App-wide offline indicator above the nav host (ticket #28).
                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineBanner()
                    AppNavGraph(
                        startDestination = startDestination,
                        darkMode = darkMode,
                        onDarkModeChange = { turnedOn ->
                            // Any manual toggle persists and overrides the device theme.
                            darkMode = turnedOn
                            themePreference.saveDarkMode(turnedOn)
                        }
                    )
                }
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

        // If the deletion confirmation link arrives while the app is already running,
        // re-evaluate the start destination so the delete flow can resume.
        val link = intent.data?.toString()
        if (link != null &&
            FirebaseAuth.getInstance().isSignInWithEmailLink(link) &&
            FirebaseAuth.getInstance().currentUser != null
        ) {
            PendingDeletionStore(this).emailLink = link
            recreate()
        }

        // Contact-save App Link opened while the app is running: re-evaluate the
        // start destination so it routes to prefilled Add Contact.
        val data = intent.data
        if (data?.host == CONTACT_LINK_HOST && data.path?.startsWith("/add") == true) {
            recreate()
        }
    }
}
