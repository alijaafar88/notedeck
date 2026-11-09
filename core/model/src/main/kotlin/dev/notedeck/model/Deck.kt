package dev.notedeck.model

data class Card(val front: String, val back: String)

data class Deck(val name: String, val cards: List<Card>)

// TODO(k2): give Card an id.
// TODO(k2): add a ReviewGrade enum: AGAIN, HARD, GOOD, EASY.
// TODO(k2): add a sealed ReviewResult with one case for a correct answer and one for a miss.
