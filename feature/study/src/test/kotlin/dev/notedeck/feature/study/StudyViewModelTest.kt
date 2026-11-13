package dev.notedeck.feature.study

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import dev.notedeck.core.testing.MainDispatcherRule
import dev.notedeck.domain.FakeDeckRepository
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import dev.notedeck.model.Review
import dev.notedeck.model.ReviewGrade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

/** The store around the reducer: loading from the repository, saved state and effects. */
class StudyViewModelTest {
    private val deck = Deck("Kotlin", listOf(Card("1", "Q1", "A1"), Card("2", "Q2", "A2")))
    private val repository = FakeDeckRepository(listOf(deck))
    private val now = Instant.parse("2026-11-12T09:00:00Z")
    private val clock = object : Clock {
        override fun now() = this@StudyViewModelTest.now
    }

    // Unconfined: the deck loads as soon as the ViewModel starts, so intents can follow at once.
    @get:Rule
    val mainDispatcher = MainDispatcherRule(UnconfinedTestDispatcher())

    @Test
    fun `intents move through the deck`() = runTest {
        val viewModel = StudyViewModel("Kotlin", SavedStateHandle(), repository, clock)
        viewModel.uiState.test {
            assertEquals(StudyUiState.Question("Q1", "A1", revealed = false, position = 1, total = 2), awaitLoaded())
            viewModel.dispatch(StudyIntent.Reveal)
            assertEquals(StudyUiState.Question("Q1", "A1", revealed = true, position = 1, total = 2), awaitItem())
            viewModel.dispatch(StudyIntent.Answer(knewIt = true))
            assertEquals(StudyUiState.Question("Q2", "A2", revealed = false, position = 2, total = 2), awaitItem())
            viewModel.dispatch(StudyIntent.Answer(knewIt = false))
            assertEquals(StudyUiState.Finished(known = 1, total = 2), awaitItem())
        }
    }

    @Test
    fun `done sends one Close effect`() = runTest {
        val viewModel = StudyViewModel("Kotlin", SavedStateHandle(), repository, clock)
        viewModel.dispatch(StudyIntent.Done)
        viewModel.effects.test {
            assertEquals(StudyEffect.Close, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `after process death the session resumes from saved state`() = runTest {
        val saved = SavedStateHandle()
        StudyViewModel("Kotlin", saved, repository, clock).dispatch(StudyIntent.Answer(knewIt = true))

        StudyViewModel("Kotlin", saved, repository, clock).uiState.test {
            assertEquals(StudyUiState.Question("Q2", "A2", revealed = false, position = 2, total = 2), awaitLoaded())
        }
    }

    @Test
    fun `no deck chosen asks the user to pick one`() = runTest {
        StudyViewModel(null, SavedStateHandle(), repository, clock).uiState.test {
            assertEquals(StudyUiState.NoDeck, awaitLoaded())
        }
    }

    @Test
    fun `each answer is recorded once, with the clock's time`() = runTest {
        val viewModel = StudyViewModel("Kotlin", SavedStateHandle(), repository, clock)
        viewModel.dispatch(StudyIntent.Answer(knewIt = true))
        viewModel.dispatch(StudyIntent.Answer(knewIt = false))
        // The session is finished: a third answer changes nothing and records nothing.
        viewModel.dispatch(StudyIntent.Answer(knewIt = true))
        assertEquals(listOf(Review("1", ReviewGrade.GOOD, now), Review("2", ReviewGrade.AGAIN, now)), repository.reviews)
    }
}

/** Skips the initial Loading, which an Unconfined collector may or may not see before the first real value. */
private suspend fun ReceiveTurbine<StudyUiState>.awaitLoaded(): StudyUiState = awaitItem().let { if (it == StudyUiState.Loading) awaitItem() else it }
