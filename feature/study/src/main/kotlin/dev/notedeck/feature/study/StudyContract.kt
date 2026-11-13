package dev.notedeck.feature.study

import dev.notedeck.model.Deck

/** The whole study session in one value, so related fields always change together. */
data class StudyState(
    val deck: Deck? = null,
    val loading: Boolean = true,
    val position: Int = 0,
    val known: Int = 0,
    val revealed: Boolean = false,
) {
    val finished: Boolean get() = deck != null && position >= deck.cards.size
}

sealed interface StudyIntent {
    /** The repository's answer arrives as an intent too, so every change goes through [reduce]. */
    data class DeckLoaded(val deck: Deck?) : StudyIntent

    data object Reveal : StudyIntent

    data class Answer(val knewIt: Boolean) : StudyIntent

    data object Restart : StudyIntent

    data object Done : StudyIntent
}

sealed interface StudyEffect {
    data object Close : StudyEffect
}

/** Pure: no repository, no clock, no coroutines. */
fun reduce(state: StudyState, intent: StudyIntent): StudyState = when (intent) {
    is StudyIntent.DeckLoaded -> state.copy(deck = intent.deck, loading = false)
    StudyIntent.Reveal -> if (state.finished) state else state.copy(revealed = true)
    is StudyIntent.Answer -> if (state.finished) {
        state
    } else {
        state.copy(position = state.position + 1, known = state.known + if (intent.knewIt) 1 else 0, revealed = false)
    }
    StudyIntent.Restart -> state.copy(position = 0, known = 0, revealed = false)
    StudyIntent.Done -> state
}

sealed interface StudyUiState {
    data object Loading : StudyUiState

    data object NoDeck : StudyUiState

    data class Question(val front: String, val back: String, val revealed: Boolean, val position: Int, val total: Int) : StudyUiState

    data class Finished(val known: Int, val total: Int) : StudyUiState
}

fun StudyState.toUiState(): StudyUiState = when {
    loading -> StudyUiState.Loading
    deck == null -> StudyUiState.NoDeck
    finished -> StudyUiState.Finished(known, deck.cards.size)
    else -> deck.cards[position].let { StudyUiState.Question(it.front, it.back, revealed, position + 1, deck.cards.size) }
}
