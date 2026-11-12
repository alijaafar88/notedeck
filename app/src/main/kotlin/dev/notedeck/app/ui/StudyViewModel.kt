package dev.notedeck.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.notedeck.app.ServiceLocator
import dev.notedeck.domain.DeckRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/** MVI: every change is an intent through [reduce]; one-off effects go through a Channel. */
class StudyViewModel(
    private val deckName: String?,
    private val saved: SavedStateHandle,
    repository: DeckRepository,
) : ViewModel() {
    private val state = MutableStateFlow(StudyState(position = saved[POSITION] ?: 0, known = saved[KNOWN] ?: 0))

    val uiState: StateFlow<StudyUiState> = state
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyUiState.Loading)

    private val _effects = Channel<StudyEffect>(Channel.BUFFERED)
    val effects: Flow<StudyEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.decks
                .map { decks -> decks.firstOrNull { it.name == deckName } }
                .distinctUntilChanged()
                .collect { dispatch(StudyIntent.DeckLoaded(it)) }
        }
    }

    fun dispatch(intent: StudyIntent) {
        val next = state.updateAndGet { reduce(it, intent) }
        // Saved so a process death mid-session resumes at the same card.
        saved[POSITION] = next.position
        saved[KNOWN] = next.known
        if (intent == StudyIntent.Done) _effects.trySend(StudyEffect.Close)
    }

    companion object {
        private const val POSITION = "position"
        private const val KNOWN = "known"

        fun factory(deckName: String?) = viewModelFactory {
            initializer { StudyViewModel(deckName, createSavedStateHandle(), ServiceLocator.decks) }
        }
    }
}
