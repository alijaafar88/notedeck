package dev.notedeck.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DeckTest {
    private val card = Card("1", "front", "back")

    @Test
    fun `copy changes only the named property`() {
        val edited = card.copy(back = "new back")
        assertEquals("new back", edited.back)
        assertEquals(card.front, edited.front)
        assertEquals(card.id, edited.id)
    }

    @Test
    fun `cards with the same values are equal`() {
        assertEquals(card, Card("1", "front", "back"))
        assertNotEquals(card, card.copy(id = "2"))
    }

    @Test
    fun `decks compare by their cards`() {
        assertEquals(Deck("d", listOf(card)), Deck("d", listOf(card.copy())))
        assertNotEquals(Deck("d", listOf(card)), Deck("d", emptyList()))
    }

    @Test
    fun `a when over ReviewResult needs no else`() {
        val results = listOf(ReviewResult.Correct(card, ReviewGrade.GOOD), ReviewResult.Missed(card))
        val labels = results.map { result ->
            when (result) {
                is ReviewResult.Correct -> result.grade.name
                is ReviewResult.Missed -> "MISSED"
            }
        }
        assertEquals(listOf("GOOD", "MISSED"), labels)
    }
}
