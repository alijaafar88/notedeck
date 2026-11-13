package dev.notedeck.app.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import dev.notedeck.app.testing.MainDispatcherRule
import dev.notedeck.domain.FakeDeckRepository
import dev.notedeck.model.Deck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class DecksViewModelTest {
    // A StandardTestDispatcher: coroutines run only when the test waits, so the order of states is exact.
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    // The same dispatcher stands in for Dispatchers.IO, so the test controls every thread the ViewModel uses.
    private fun viewModel(repository: FakeDeckRepository) = DecksViewModel(repository, mainDispatcher.dispatcher)

    @Test
    fun `is Loading until someone collects`() {
        assertEquals(DecksUiState.Loading, viewModel(FakeDeckRepository()).uiState.value)
    }

    @Test
    fun `the states arrive in order - Loading, Empty, then Content`() = runTest {
        val repository = FakeDeckRepository()
        viewModel(repository).uiState.test {
            assertEquals(DecksUiState.Loading, awaitItem())
            assertEquals(DecksUiState.Empty, awaitItem())
            val deck = Deck("Kotlin", emptyList())
            repository.addDeck(deck)
            assertEquals(DecksUiState.Content(listOf(deck)), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `addCard puts a trimmed card in the named deck`() = runTest {
        val repository = FakeDeckRepository(listOf(Deck("Kotlin", emptyList())))
        viewModel(repository).addCard("Kotlin", "  val?  ", " read-only ")
        advanceUntilIdle()
        repository.decks.test {
            val card = awaitItem().single().cards.single()
            assertEquals("val?" to "read-only", card.front to card.back)
        }
    }

    @Test
    fun `cardCount reads naturally`() {
        assertEquals(listOf("0 cards", "1 card", "2 cards"), listOf(0, 1, 2).map(::cardCount))
    }
}

/** Skips the initial Loading, which an Unconfined collector may or may not see before the first real value. */
suspend fun <T> ReceiveTurbine<T>.awaitLoaded(): T = awaitItem().let { if (it == DecksUiState.Loading || it == StudyUiState.Loading) awaitItem() else it }
