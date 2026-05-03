package com.humblesolutions.humblecontacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.humblecontacts.navigation.AppNavGraph
import com.humblesolutions.humblecontacts.navigation.Routes
import com.humblesolutions.humblecontacts.ui.theme.HumbleContactsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
7
        setContent {
            HumbleContactsTheme {
                val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

                AppNavGraph(
                    startDestination = if (isLoggedIn) Routes.HOME else Routes.SPLASH
                )
            }
        }
    }
}