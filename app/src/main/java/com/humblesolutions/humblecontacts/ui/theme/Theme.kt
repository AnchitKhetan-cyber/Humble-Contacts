package com.humblesolutions.humblecontacts.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ui/theme/Theme.kt  (color scheme section only)

private val LightColorScheme = lightColorScheme(
    primary            = Navy600,
    onPrimary          = TextOnNavy,
    primaryContainer   = Navy50,
    onPrimaryContainer = Navy800,

    secondary            = Gold400,
    onSecondary          = TextOnGold,
    secondaryContainer   = Gold50,
    onSecondaryContainer = Gold800,

    background    = AppBackground,
    onBackground  = TextPrimary,
    surface       = SurfaceLight,
    onSurface     = TextPrimary,
    surfaceVariant    = SurfaceVariant,
    onSurfaceVariant  = TextSecondary,

    error   = Error,
    onError = White,
    errorContainer   = ErrorBg,
    onErrorContainer = Color(0xFF690005),

    outline      = Navy200,
    outlineVariant = Navy100,
)

private val DarkColorScheme = darkColorScheme(
    primary            = Navy200,
    onPrimary          = Navy900,
    primaryContainer   = Navy800,
    onPrimaryContainer = Navy50,

    secondary            = Gold200,
    onSecondary          = Gold900,
    secondaryContainer   = Gold800,
    onSecondaryContainer = Gold100,

    background    = DarkBackground,
    onBackground  = TextDark,
    surface       = DarkSurface,
    onSurface     = TextDark,
    surfaceVariant    = DarkSurfaceVariant,
    onSurfaceVariant  = TextDarkMuted,

    error   = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer   = Color(0xFF93000A),
    onErrorContainer = ErrorBg,

    outline      = Navy400,
    outlineVariant = Navy800,
)

@Composable
fun HumbleContactsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}