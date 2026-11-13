package dev.notedeck.domain

import app.cash.turbine.test
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class FakeDeckRepositoryTest {
    @Test
    fun `addDeck emits the updated list`() = runTest {
        val repository = FakeDeckRepository()
        val deck = Deck("Kotlin basics", emptyList())

        repository.decks.test {
            assertEquals(emptyList(), awaitItem())
            repository.addDeck(deck)
            assertEquals(listOf(deck), awaitItem())
            repository.addDeck(deck.copy(name = "Flow"))
            assertEquals(listOf("Kotlin basics", "Flow"), awaitItem().map { it.name })
        }
    }

    @Test
    fun `addCard appends to the named deck only`() = runTest {
        val repository = FakeDeckRepository(listOf(Deck("Kotlin", emptyList()), Deck("Flow", emptyList())))
        repository.addCard("Kotlin", Card("1", "val?", "read-only"))
        repository.decks.test {
            assertEquals(listOf(1, 0), awaitItem().map { it.cards.size })
        }
    }
}
