package com.humblesolutions.humblecontacts.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        startSplashSequence()

    }

    private fun startSplashSequence() {
        viewModelScope.launch {
            delay(100)                          // wait for first frame
            _uiState.update { it.copy(started = true) }

            delay(2800)                         // animation finishes
            _uiState.update {
                it.copy(destination = SplashDestination.Onboard)
            }
        }
    }

    /** Called by the UI AFTER it has acted on a navigation event, to reset it. */
    fun onNavigated() {
        _uiState.update { it.copy(destination = SplashDestination.None) }
    }
}