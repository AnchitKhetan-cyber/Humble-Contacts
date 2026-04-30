// ui/splash/SplashUiState.kt
package com.humblesolutions.humblecontacts.ui.splash

/**
 * Represents everything the UI needs to render and react to.
 * The View never makes timing decisions — it just reads this.
 */
data class SplashUiState(
    val started: Boolean = false,          // gates animations (was your `var started`)
    val destination: SplashDestination = SplashDestination.None
)

sealed interface SplashDestination {
    data object None    : SplashDestination   // stay on splash
    data object Home    : SplashDestination   // go to contacts list
    data object Onboard : SplashDestination   // first launch
}