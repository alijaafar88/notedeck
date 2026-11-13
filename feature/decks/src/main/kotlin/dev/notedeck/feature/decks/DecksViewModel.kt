package dev.notedeck.feature.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.domain.DeckRepository
import dev.notedeck.domain.IoDispatcher
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DecksUiState {
    data object Loading : DecksUiState

    data class Content(val decks: List<Deck>) : DecksUiState

    data object Empty : DecksUiState
}

@HiltViewModel
class DecksViewModel @Inject constructor(
    private val repository: DeckRepository,
    @IoDispatcher io: CoroutineDispatcher,
) : ViewModel() {
    val uiState: StateFlow<DecksUiState> = repository.decks
        .map { decks -> if (decks.isEmpty()) DecksUiState.Empty else DecksUiState.Content(decks) }
        // Reading storage stays off the main thread; tests pass a test dispatcher instead.
        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DecksUiState.Loading)

    /** A saved card must reach Room even if the learner leaves the screen at once, so the write can't be cancelled. */
    fun addCard(deckName: String, front: String, back: String) {
        viewModelScope.launch {
            withContext(NonCancellable) { repository.addCard(deckName, Card(UUID.randomUUID().toString(), front.trim(), back.trim())) }
        }
    }
}
