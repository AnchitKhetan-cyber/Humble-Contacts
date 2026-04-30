package com.humblesolutions.humblecontacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.humblesolutions.humblecontacts.navigation.AppNavGraph
import com.humblesolutions.humblecontacts.ui.auth.AuthViewModel
import com.humblesolutions.humblecontacts.ui.theme.HumbleContactsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HumbleContactsTheme {
                AppNavGraph(
                    onAuthComplete = {
                        // Post-login side-effects here if needed
                    }
                )
            }
        }
    }
}
