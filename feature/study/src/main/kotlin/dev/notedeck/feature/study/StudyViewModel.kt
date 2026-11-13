package dev.notedeck.feature.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.domain.DeckRepository
import dev.notedeck.model.Review
import dev.notedeck.model.ReviewGrade
import kotlin.time.Clock
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

/**
 * MVI: every change is an intent through [reduce]; one-off effects go through a Channel.
 * The deck name comes from the navigation key, so it's an assisted parameter; Hilt supplies the rest.
 * Each answer is recorded as a review, stamped by the injected clock; the reducer stays pure.
 */
@HiltViewModel(assistedFactory = StudyViewModel.Factory::class)
class StudyViewModel @AssistedInject constructor(
    @Assisted private val deckName: String?,
    private val saved: SavedStateHandle,
    private val repository: DeckRepository,
    private val clock: Clock,
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
        val before = state.value
        val next = state.updateAndGet { reduce(it, intent) }
        if (intent is StudyIntent.Answer && !before.finished) record(before, intent.knewIt)
        // Saved so a process death mid-session resumes at the same card.
        saved[POSITION] = next.position
        saved[KNOWN] = next.known
        if (intent == StudyIntent.Done) _effects.trySend(StudyEffect.Close)
    }

    private fun record(state: StudyState, knewIt: Boolean) {
        val card = state.deck?.cards?.getOrNull(state.position) ?: return
        val review = Review(card.id, if (knewIt) ReviewGrade.GOOD else ReviewGrade.AGAIN, clock.now())
        viewModelScope.launch { repository.recordReview(review) }
    }

    @AssistedFactory
    interface Factory {
        fun create(deckName: String?): StudyViewModel
    }

    private companion object {
        const val POSITION = "position"
        const val KNOWN = "known"
    }
}
