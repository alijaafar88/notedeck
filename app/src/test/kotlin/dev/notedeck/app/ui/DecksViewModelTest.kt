package dev.notedeck.app.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import dev.notedeck.domain.FakeDeckRepository
import dev.notedeck.model.Deck
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class DecksViewModelTest {
    // Injected in place of Dispatchers.IO, so the test controls every thread the ViewModel uses.
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `is Loading until someone collects`() {
        assertEquals(DecksUiState.Loading, DecksViewModel(FakeDeckRepository(), dispatcher).uiState.value)
    }

    @Test
    fun `an empty repository is Empty, and adding a deck shows Content`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = DecksViewModel(repository, dispatcher)
        viewModel.uiState.test {
            assertEquals(DecksUiState.Empty, awaitLoaded())
            val deck = Deck("Kotlin", emptyList())
            repository.addDeck(deck)
            assertEquals(DecksUiState.Content(listOf(deck)), awaitItem())
        }
    }
}

/** Skips the initial Loading, which a collector may or may not see before the first real value. */
suspend fun <T> ReceiveTurbine<T>.awaitLoaded(): T = awaitItem().let { if (it == DecksUiState.Loading || it == StudyUiState.Loading) awaitItem() else it }
