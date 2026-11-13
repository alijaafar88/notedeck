package dev.notedeck.core.data

import dev.notedeck.model.Card
import dev.notedeck.model.Deck

/** Seeded into Room the first time the app runs, so a new install isn't empty. */
object SampleDecks {
    val all = listOf(
        Deck(
            "Kotlin basics",
            listOf(
                Card("1", "val or var for a constant reference?", "val"),
                Card("2", "What makes a type nullable?", "A trailing ?"),
            ),
        ),
        Deck("Coroutines", listOf(Card("3", "What does launch return?", "A Job"))),
    )
}
