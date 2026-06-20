package com.humblesolutions.humblecontacts

import android.content.Context

class ThemePreference(context: Context) {

    private val prefs =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun saveDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }
}