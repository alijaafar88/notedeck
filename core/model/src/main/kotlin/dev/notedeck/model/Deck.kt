package dev.notedeck.model

data class Card(val front: String, val back: String)

data class Deck(val name: String, val cards: List<Card>)
