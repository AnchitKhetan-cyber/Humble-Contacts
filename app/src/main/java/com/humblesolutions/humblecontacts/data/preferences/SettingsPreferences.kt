package com.humblesolutions.humblecontacts.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "settings"
)

class SettingsPreferences(
    private val context: Context
) {

    companion object {

        val NOTIFICATIONS =
            booleanPreferencesKey("notifications")

        val DARK_MODE =
            booleanPreferencesKey("dark_mode")
    }

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { pref ->
            pref[NOTIFICATIONS] ?: true
        }

    suspend fun setNotifications(
        enabled: Boolean
    ) {
        context.dataStore.edit { pref ->
            pref[NOTIFICATIONS] = enabled
        }
    }
}