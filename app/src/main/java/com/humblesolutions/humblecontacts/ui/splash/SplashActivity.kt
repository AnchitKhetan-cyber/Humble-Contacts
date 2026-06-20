package com.humblesolutions.humblecontacts.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.humblesolutions.humblecontacts.MainActivity
import com.humblesolutions.humblecontacts.ui.theme.HumbleContactsTheme

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)

        // Edge-to-edge fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller =
            WindowInsetsControllerCompat(
                window,
                window.decorView
            )

        controller.hide(
            WindowInsetsCompat.Type.statusBars()
        )

        controller.hide(
            WindowInsetsCompat.Type.navigationBars()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Prevent startup white flash
        window.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        setContent {

            HumbleContactsTheme(
                darkTheme = true
            ) {

                AnimatedSplashScreen(

                    onNavigate = {

                        navigateToMain()

                    }

                )

            }

        }

    }

    private fun navigateToMain() {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        // Prevent returning to splash
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()

    }

}