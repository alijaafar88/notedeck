package dev.notedeck.app.ui

import androidx.lifecycle.ViewModel
import dev.notedeck.domain.DeckRepository

// TODO(m1): a sealed DecksUiState with Loading, Content(decks) and Empty.

class DecksViewModel(private val repository: DeckRepository) : ViewModel() {
    // TODO(m1): val uiState: StateFlow<DecksUiState>, mapped from repository.decks and shared with
    //  stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DecksUiState.Loading).
    // TODO(m1): a factory that builds it with ServiceLocator.decks.
}
