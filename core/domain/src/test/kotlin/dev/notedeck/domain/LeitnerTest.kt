package dev.notedeck.domain

import dev.notedeck.model.Card
import dev.notedeck.model.ReviewGrade
import kotlin.test.Test
import kotlin.test.assertEquals

class LeitnerTest {
    private data class Case(val box: Int, val grade: ReviewGrade, val expected: Int)

    @Test
    fun `moves cards between boxes`() {
        listOf(
            Case(3, ReviewGrade.AGAIN, 1),
            Case(1, ReviewGrade.AGAIN, 1),
            Case(3, ReviewGrade.HARD, 3),
            Case(3, ReviewGrade.GOOD, 4),
            Case(5, ReviewGrade.GOOD, 5),
            Case(3, ReviewGrade.EASY, 5),
            Case(4, ReviewGrade.EASY, 5),
        ).forEach { case ->
            val card = Card("1", "front", "back", case.box)
            assertEquals(case.expected, nextReview(card, case.grade).box, "$case")
        }
    }

    @Test
    fun `only the box changes`() {
        val card = Card("1", "front", "back", 2)
        assertEquals(card.copy(box = 3), nextReview(card, ReviewGrade.GOOD))
    }
}
