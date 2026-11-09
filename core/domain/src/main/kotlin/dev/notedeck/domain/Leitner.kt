package dev.notedeck.domain

import dev.notedeck.model.Card
import dev.notedeck.model.ReviewGrade

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
