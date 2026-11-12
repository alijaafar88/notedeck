package dev.notedeck.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dev.notedeck.domain.DeckRepository

// TODO(m1): a sealed StudyUiState: Loading, NoDeck, Question(front, back, revealed, position, total) and Finished(known, total).

class StudyViewModel(
    private val deckName: String?,
    private val saved: SavedStateHandle,
    private val repository: DeckRepository,
) : ViewModel() {
    // TODO(m1): keep the position and the known count in SavedStateHandle, so they survive process death.
    // TODO(m1): val uiState: StateFlow<StudyUiState>, and reveal() and answer(knewIt: Boolean).
}
