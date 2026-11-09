package dev.notedeck.domain

import app.cash.turbine.test
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
}
