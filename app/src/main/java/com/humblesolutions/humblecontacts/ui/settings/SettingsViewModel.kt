package com.humblesolutions.humblecontacts.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.humblecontacts.data.preferences.SettingsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs =
        SettingsPreferences(application)

    val notificationsEnabled =
        prefs.notificationsEnabled.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    fun setNotifications(
        enabled: Boolean
    ) {
        viewModelScope.launch {
            prefs.setNotifications(enabled)
        }
    }
}