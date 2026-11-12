package dev.notedeck.app.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.notedeck.domain.FakeDeckRepository
import dev.notedeck.model.Card
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

class StudyViewModelTest {
    private val deck = Deck("Kotlin", listOf(Card("1", "Q1", "A1"), Card("2", "Q2", "A2")))
    private val repository = FakeDeckRepository().apply { addDeck(deck) }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `answering moves through the deck and counts what you knew`() = runTest {
        val viewModel = StudyViewModel("Kotlin", SavedStateHandle(), repository)
        viewModel.uiState.test {
            assertEquals(StudyUiState.Question("Q1", "A1", revealed = false, position = 1, total = 2), awaitLoaded())
            viewModel.reveal()
            assertEquals(StudyUiState.Question("Q1", "A1", revealed = true, position = 1, total = 2), awaitItem())
            viewModel.answer(knewIt = true)
            assertEquals(StudyUiState.Question("Q2", "A2", revealed = false, position = 2, total = 2), awaitItem())
            viewModel.answer(knewIt = false)
            assertEquals(StudyUiState.Finished(known = 1, total = 2), awaitItem())
        }
    }

    @Test
    fun `after process death the session resumes from saved state`() = runTest {
        val saved = SavedStateHandle()
        StudyViewModel("Kotlin", saved, repository).answer(knewIt = true)

        val restored = StudyViewModel("Kotlin", saved, repository)
        restored.uiState.test {
            assertEquals(StudyUiState.Question("Q2", "A2", revealed = false, position = 2, total = 2), awaitLoaded())
        }
    }

    @Test
    fun `no deck chosen asks the user to pick one`() = runTest {
        StudyViewModel(null, SavedStateHandle(), repository).uiState.test {
            assertEquals(StudyUiState.NoDeck, awaitLoaded())
        }
    }
}
