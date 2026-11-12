package dev.notedeck.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.notedeck.app.ServiceLocator
import dev.notedeck.domain.DeckRepository
import dev.notedeck.model.Deck
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DecksUiState {
    data object Loading : DecksUiState

    data class Content(val decks: List<Deck>) : DecksUiState

    data object Empty : DecksUiState
}

class DecksViewModel(repository: DeckRepository) : ViewModel() {
    val uiState: StateFlow<DecksUiState> = repository.decks
        .map { decks -> if (decks.isEmpty()) DecksUiState.Empty else DecksUiState.Content(decks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DecksUiState.Loading)

    companion object {
        val Factory = viewModelFactory { initializer { DecksViewModel(ServiceLocator.decks) } }
    }
}
