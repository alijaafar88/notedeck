package dev.notedeck.model

data class Card(val id: String, val front: String, val back: String)

data class Deck(val name: String, val cards: List<Card>)

enum class ReviewGrade { AGAIN, HARD, GOOD, EASY }

sealed interface ReviewResult {
    val card: Card

    data class Correct(override val card: Card, val grade: ReviewGrade) : ReviewResult

    data class Missed(override val card: Card) : ReviewResult
}
