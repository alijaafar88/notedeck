package dev.notedeck.cli

import dev.notedeck.model.Card
import dev.notedeck.model.Deck

fun main() {
    val deck = Deck(
        name = "Kotlin basics",
        cards = listOf(
            Card("1", "val or var for a constant reference?", "val"),
            Card("2", "What makes a type nullable?", "A trailing ?"),
        ),
    )
    println("${deck.name} (${deck.cards.size} cards)")
    deck.cards.forEach { card -> println("- ${card.front} -> ${card.back}") }
}
