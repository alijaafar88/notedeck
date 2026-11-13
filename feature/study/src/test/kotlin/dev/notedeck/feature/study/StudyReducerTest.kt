package dev.notedeck.feature.study

import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import kotlin.test.Test
import kotlin.test.assertEquals

/** One row per intent and edge case: the reducer is pure, so no fakes or dispatchers. */
class StudyReducerTest {
    private val deck = Deck("Kotlin", listOf(Card("1", "Q1", "A1"), Card("2", "Q2", "A2")))
    private val ready = StudyState(deck = deck, loading = false)

    private data class Case(val name: String, val before: StudyState, val intent: StudyIntent, val after: StudyState)

    private val cases = listOf(
        Case("the deck arrives", StudyState(), StudyIntent.DeckLoaded(deck), ready),
        Case("no such deck", StudyState(), StudyIntent.DeckLoaded(null), StudyState(loading = false)),
        Case("reveal shows the answer", ready, StudyIntent.Reveal, ready.copy(revealed = true)),
        Case("knew it moves on and counts", ready.copy(revealed = true), StudyIntent.Answer(knewIt = true), ready.copy(position = 1, known = 1)),
        Case("missed it moves on", ready.copy(revealed = true), StudyIntent.Answer(knewIt = false), ready.copy(position = 1)),
        Case("answers after the end are ignored", ready.copy(position = 2, known = 1), StudyIntent.Answer(knewIt = true), ready.copy(position = 2, known = 1)),
        Case("reveal after the end is ignored", ready.copy(position = 2), StudyIntent.Reveal, ready.copy(position = 2)),
        Case("restart starts over", ready.copy(position = 2, known = 2), StudyIntent.Restart, ready),
        Case("done changes nothing; it's an effect", ready.copy(position = 2), StudyIntent.Done, ready.copy(position = 2)),
    )

    @Test
    fun `every case`() = cases.forEach { assertEquals(it.after, reduce(it.before, it.intent), it.name) }

    @Test
    fun `the screen sees the right state`() {
        assertEquals(StudyUiState.Loading, StudyState().toUiState())
        assertEquals(StudyUiState.NoDeck, StudyState(loading = false).toUiState())
        assertEquals(StudyUiState.Question("Q2", "A2", revealed = false, position = 2, total = 2), ready.copy(position = 1).toUiState())
        assertEquals(StudyUiState.Finished(known = 1, total = 2), ready.copy(position = 2, known = 1).toUiState())
    }
}
