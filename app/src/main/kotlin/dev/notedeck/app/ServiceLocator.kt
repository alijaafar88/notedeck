package dev.notedeck.app

import dev.notedeck.domain.DeckRepository
import dev.notedeck.domain.FakeDeckRepository

/** One shared repository until M3 replaces this with an AppContainer, and then with Hilt. */
object ServiceLocator {
    val decks: DeckRepository by lazy { FakeDeckRepository().apply { SampleDecks.all.forEach(::addDeck) } }
}
