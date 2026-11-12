package dev.notedeck.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.notedeck.app.ServiceLocator
import dev.notedeck.domain.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface StudyUiState {
    data object Loading : StudyUiState

    data object NoDeck : StudyUiState

    data class Question(val front: String, val back: String, val revealed: Boolean, val position: Int, val total: Int) : StudyUiState

    data class Finished(val known: Int, val total: Int) : StudyUiState
}

class StudyViewModel(
    private val deckName: String?,
    private val saved: SavedStateHandle,
    repository: DeckRepository,
) : ViewModel() {
    /** One value, so a screen never sees the position moved but the answer still showing. */
    private data class Progress(val position: Int, val known: Int, val revealed: Boolean = false)

    private val progress = MutableStateFlow(Progress(saved[POSITION] ?: 0, saved[KNOWN] ?: 0))

    val uiState: StateFlow<StudyUiState> = combine(repository.decks, progress) { decks, (position, known, revealed) ->
        val deck = decks.firstOrNull { it.name == deckName }
        when {
            deck == null -> StudyUiState.NoDeck
            position >= deck.cards.size -> StudyUiState.Finished(known, deck.cards.size)
            else -> deck.cards[position].let { StudyUiState.Question(it.front, it.back, revealed, position + 1, deck.cards.size) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyUiState.Loading)

    fun reveal() = progress.update { it.copy(revealed = true) }

    fun answer(knewIt: Boolean) {
        progress.update { it.copy(position = it.position + 1, known = it.known + if (knewIt) 1 else 0, revealed = false) }
        // Saved so a process death mid-session resumes at the same card.
        saved[POSITION] = progress.value.position
        saved[KNOWN] = progress.value.known
    }

    companion object {
        private const val POSITION = "position"
        private const val KNOWN = "known"

        fun factory(deckName: String?) = viewModelFactory {
            initializer { StudyViewModel(deckName, createSavedStateHandle(), ServiceLocator.decks) }
        }
    }
}
