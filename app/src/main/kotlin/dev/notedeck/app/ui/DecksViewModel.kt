package dev.notedeck.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.app.di.IoDispatcher
import dev.notedeck.domain.DeckRepository
import dev.notedeck.model.Deck
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DecksUiState {
    data object Loading : DecksUiState

    data class Content(val decks: List<Deck>) : DecksUiState

    data object Empty : DecksUiState
}

@HiltViewModel
class DecksViewModel @Inject constructor(
    repository: DeckRepository,
    @IoDispatcher io: CoroutineDispatcher,
) : ViewModel() {
    val uiState: StateFlow<DecksUiState> = repository.decks
        .map { decks -> if (decks.isEmpty()) DecksUiState.Empty else DecksUiState.Content(decks) }
        // Reading storage stays off the main thread; tests pass a test dispatcher instead.
        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DecksUiState.Loading)
}
