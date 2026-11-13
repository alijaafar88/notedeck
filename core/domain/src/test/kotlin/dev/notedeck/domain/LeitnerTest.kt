package dev.notedeck.domain

import dev.notedeck.model.Card
import dev.notedeck.model.ReviewGrade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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

    @Test
    fun `intervals double from box to box`() {
        assertEquals(listOf(1, 2, 4, 8, 16), (FIRST_BOX..LAST_BOX).map(::reviewIntervalDays))
    }

    @Test
    fun `a card is due when its interval has passed, or it was never reviewed`() {
        val now = Instant.parse("2026-11-12T09:00:00Z")
        assertTrue(isDue(box = 1, lastReviewedAt = null, now = now))
        assertTrue(isDue(box = 2, lastReviewedAt = now - 2.days, now = now))
        assertFalse(isDue(box = 2, lastReviewedAt = now - 47.hours, now = now))
        assertFalse(isDue(box = 5, lastReviewedAt = now - 15.days, now = now))
    }

    @Test
    fun `boxes outside 1 to 5 use the nearest box's interval`() {
        assertEquals(listOf(1, 1, 16, 16), listOf(-3, 0, 6, 99).map(::reviewIntervalDays))
    }

    @Test
    fun `in every box, a card falls due exactly one interval after its review`() {
        val now = Instant.parse("2026-11-12T09:00:00Z")
        for (box in FIRST_BOX..LAST_BOX) {
            val interval = reviewIntervalDays(box).days
            assertTrue(isDue(box, lastReviewedAt = now - interval, now = now), "box $box, on time")
            assertFalse(isDue(box, lastReviewedAt = now - interval + 1.seconds, now = now), "box $box, a second early")
        }
    }
}
