package com.humblesolutions.humblecontacts.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblesolutions.humblecontacts.data.model.UserProfile
import com.humblesolutions.humblecontacts.data.model.VisitingCard
import com.humblesolutions.humblecontacts.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the visiting-card editor (#64): loads the user's profile + saved card,
 * holds the in-progress edits, and persists them. Keeps all logic out of the
 * composables — the screen only renders [uiState] and calls these methods.
 */
class VisitingCardViewModel : ViewModel() {

    private val repository = ProfileRepository()

    private val _uiState = MutableStateFlow(VisitingCardUiState())
    val uiState: StateFlow<VisitingCardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = false) }
            try {
                val profile = repository.getCurrentUserProfile()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        card = profile?.visitingCard ?: VisitingCard(),
                        loadError = profile == null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = true) }
            }
        }
    }

    // ── Edits (mutate only the local card copy) ──────────────────────────────
    private fun updateCard(transform: (VisitingCard) -> VisitingCard) {
        _uiState.update { it.copy(card = transform(it.card), saveSuccess = false) }
    }

    fun onTemplateChange(id: String) = updateCard { it.copy(template = id) }
    fun onAccentChange(hex: String) = updateCard { it.copy(accentColor = hex) }
    fun onBackgroundChange(id: String) = updateCard { it.copy(background = id) }
    fun onFontChange(id: String) = updateCard { it.copy(fontStyle = id) }
    fun onHeadlineChange(v: String) = updateCard { it.copy(headline = v) }
    fun onBioChange(v: String) = updateCard { it.copy(bio = v) }
    fun onWebsiteChange(v: String) = updateCard { it.copy(websiteUrl = v) }
    fun onPortfolioChange(v: String) = updateCard { it.copy(portfolioUrl = v) }

    fun save() {
        val card = _uiState.value.card
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = false, saveSuccess = false) }
            try {
                repository.updateVisitingCard(card)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }

    fun consumeSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }
    fun consumeSaveError() = _uiState.update { it.copy(saveError = false) }
}

data class VisitingCardUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val saveSuccess: Boolean = false,
    val profile: UserProfile? = null,
    val card: VisitingCard = VisitingCard()
)
