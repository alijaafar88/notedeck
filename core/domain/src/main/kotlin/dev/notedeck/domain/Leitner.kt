package dev.notedeck.domain

import dev.notedeck.model.Card
import dev.notedeck.model.ReviewGrade
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

const val FIRST_BOX = 1
const val LAST_BOX = 5

fun nextReview(card: Card, grade: ReviewGrade): Card {
    val box = when (grade) {
        ReviewGrade.AGAIN -> FIRST_BOX
        ReviewGrade.HARD -> card.box
        ReviewGrade.GOOD -> card.box + 1
        ReviewGrade.EASY -> card.box + 2
    }
    return card.copy(box = box.coerceIn(FIRST_BOX, LAST_BOX))
}

/** Days until a card in [box] is due again: 1, 2, 4, 8 and 16. */
fun reviewIntervalDays(box: Int): Int = 1 shl (box.coerceIn(FIRST_BOX, LAST_BOX) - FIRST_BOX)

/** Due if never reviewed, or if its box's interval has passed since the last review. */
fun isDue(box: Int, lastReviewedAt: Instant?, now: Instant): Boolean =
    lastReviewedAt == null || now - lastReviewedAt >= reviewIntervalDays(box).days
