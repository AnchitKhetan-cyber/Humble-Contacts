package com.humblesolutions.humblecontacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

            val themePreference = ThemePreference(this)

            var darkMode by remember {
                mutableStateOf(themePreference.isDarkMode())
            }

            HumbleContactsTheme(
                darkTheme = darkMode
            ) {

                val isLoggedIn =
                    FirebaseAuth.getInstance().currentUser != null

                AppNavGraph(
                    startDestination =
                        if (isLoggedIn) Routes.HOME
                        else Routes.SPLASH,

                    darkMode = darkMode,

                    onDarkModeChange = {

                        darkMode = it

                        themePreference.saveDarkMode(it)
                    }
                )
            }
        }
    }
}