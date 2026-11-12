package dev.notedeck.app

import dev.notedeck.domain.DeckRepository
import dev.notedeck.domain.FakeDeckRepository

/** One shared repository. TODO(m3): replace with an AppContainer in NoteDeckApplication, then with Hilt. */
object ServiceLocator {
    val decks: DeckRepository by lazy { FakeDeckRepository().apply { SampleDecks.all.forEach(::addDeck) } }
}
