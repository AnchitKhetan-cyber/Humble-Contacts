package com.humblesolutions.humblecontacts.ui.intro

// ─────────────────────────────────────────────────────────────────────────────
// IntroViewModel.kt — Humble Contacts
//
// Drives the 3-page onboarding flow. Holds current page index in StateFlow.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Onboarding page data model ────────────────────────────────────────────────

data class IntroPage(
    val index:       Int,
    val headline:    String,
    val subline:     String,
    val bodyText:    String,
    val ctaLabel:    String,
    val iconTag:     String,      // used to pick the illustration composable
)

val introPages = listOf(
    IntroPage(
        index     = 0,
        headline  = "Your Network,\nYour Privacy.",
        subline   = "All connections. Zero compromise.",
        bodyText  = "Humble Contacts is a private-first network manager. Every person you meet, every conversation you have — captured securely and visible only to you.",
        ctaLabel  = "Next",
        iconTag   = "network",
    ),
    IntroPage(
        index     = 1,
        headline  = "Capture the\nFull Context.",
        subline   = "Beyond a name and number.",
        bodyText  = "Log where you met, what you discussed, attach voice notes or a selfie. Scan business cards instantly with AI. Never forget a face or a follow-up.",
        ctaLabel  = "Next",
        iconTag   = "capture",
    ),
    IntroPage(
        index     = 2,
        headline  = "Stay Connected.\nStay Ahead.",
        subline   = "Smart reminders. Real insights.",
        bodyText  = "Get follow-up reminders, see your networking patterns, and share your digital profile via NFC or QR — no paper cards needed.",
        ctaLabel  = "Get Started",
        iconTag   = "insights",
    ),
)

// ── UI State ──────────────────────────────────────────────────────────────────

data class IntroUiState(
    val currentPage:   Int     = 0,
    val totalPages:    Int     = introPages.size,
    val isLastPage:    Boolean = false,
)

sealed class IntroEvent {
    object NavigateToAuth : IntroEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class IntroViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IntroUiState())
    val uiState: StateFlow<IntroUiState> = _uiState.asStateFlow()

    private val _events = Channel<IntroEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onPageChanged(page: Int) {
        _uiState.update {
            it.copy(
                currentPage = page,
                isLastPage  = page == introPages.lastIndex
            )
        }
    }

    fun onCtaClick() {
        val state = _uiState.value
        if (state.isLastPage) {
            viewModelScope.launch { _events.send(IntroEvent.NavigateToAuth) }
        }
        // If not last page, the Pager handles advancing via user swipe or button
    }

    fun skip() {
        viewModelScope.launch { _events.send(IntroEvent.NavigateToAuth) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == IntroViewModel::class.java)
            return IntroViewModel() as T
        }
    }
}