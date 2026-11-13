package dev.notedeck.feature.assistant

import dev.notedeck.core.testing.MainDispatcherRule
import dev.notedeck.domain.Answerer
import dev.notedeck.domain.Coach
import dev.notedeck.domain.DeckStats
import dev.notedeck.domain.Grounding
import dev.notedeck.domain.NoteIndex
import dev.notedeck.domain.Passage
import dev.notedeck.domain.PlanStore
import dev.notedeck.domain.StatsSource
import dev.notedeck.domain.StudySnapshot
import dev.notedeck.domain.WeeklyPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class AssistantViewModelsTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private class FixedNotes(private val passages: List<Passage>) : NoteIndex {
        override suspend fun add(deckName: String, text: String) = Unit

        override suspend fun search(query: String, k: Int) = passages.take(k)
    }

    private class RecordingAnswerer(private vararg val pieces: String) : Answerer {
        val sent = mutableListOf<List<String>>()

        override fun answer(question: String, sources: List<String>): Flow<String> {
            sent += sources
            return flowOf(*pieces)
        }
    }

    private val channel = Passage(1, "Coroutines", "Channel: a hot pipe between coroutines", score = 0.45f)
    private val weak = Passage(2, "Kotlin basics", "val is read-only", score = 0.05f)

    @Test
    fun `only relevant passages are sent, and cited ones are marked`() = runTest {
        val answerer = RecordingAnswerer("A hot pipe ", "between coroutines [1].")
        val viewModel = AskViewModel(FixedNotes(listOf(channel, weak)), answerer)
        viewModel.onQuestionChange("What is a channel?")
        viewModel.ask()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(listOf(listOf(channel.text)), answerer.sent)
        assertEquals("A hot pipe between coroutines [1].", state.answer)
        assertEquals(true, state.grounded)
        assertEquals(listOf(SourceUi(1, "Coroutines", channel.text, cited = true)), state.sources)
        assertFalse(state.asking)
    }

    @Test
    fun `with nothing relevant, the answer is Not in your notes, and no model is called`() = runTest {
        val answerer = RecordingAnswerer("anything")
        val viewModel = AskViewModel(FixedNotes(listOf(weak)), answerer)
        viewModel.onQuestionChange("Who created Kotlin?")
        viewModel.ask()
        advanceUntilIdle()
        assertEquals(Grounding.NOT_IN_NOTES, viewModel.uiState.value.answer)
        assertEquals(emptyList(), answerer.sent)
    }

    @Test
    fun `an answer that cites nothing is flagged`() = runTest {
        val viewModel = AskViewModel(FixedNotes(listOf(channel)), RecordingAnswerer("Channels are hot."))
        viewModel.onQuestionChange("What is a channel?")
        viewModel.ask()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.grounded)
        assertTrue(viewModel.uiState.value.sources.none { it.cited })
    }

    private val snapshot = StudySnapshot(20, listOf(DeckStats("Coroutines", 6, 5, 2, 0), DeckStats("Kotlin basics", 4, 0, 9, 1)), List(7) { 2 })

    private class MemoryPlans : PlanStore {
        override val active = MutableStateFlow<WeeklyPlan?>(null)

        override suspend fun apply(plan: WeeklyPlan) {
            active.value = plan
        }
    }

    @Test
    fun `a proposal is validated and changes nothing until applied`() = runTest {
        val plans = MemoryPlans()
        val coach = Coach { WeeklyPlan(25, listOf("Coroutines", "Not a deck"), "Five cards are due there.") }
        val viewModel = CoachViewModel(StatsSource { snapshot }, coach, plans)
        viewModel.propose()
        advanceUntilIdle()
        assertEquals(WeeklyPlan(25, listOf("Coroutines"), "Five cards are due there."), viewModel.uiState.value.proposal)
        assertNull(plans.active.value, "nothing applied yet")

        viewModel.apply()
        advanceUntilIdle()
        assertEquals(WeeklyPlan(25, listOf("Coroutines"), "Five cards are due there."), plans.active.value)
        assertEquals("Plan applied: 25 cards a day.", viewModel.uiState.value.message)
        assertEquals(plans.active.value, viewModel.uiState.value.active)
        assertNull(viewModel.uiState.value.proposal)
    }

    @Test
    fun `a nonsense plan is refused with a reason, and Not now drops a proposal`() = runTest {
        val plans = MemoryPlans()
        val viewModel = CoachViewModel(StatsSource { snapshot }, Coach { WeeklyPlan(900, listOf("Coroutines"), "x") }, plans)
        viewModel.propose()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.error!!.contains("900"))
        assertNull(viewModel.uiState.value.proposal)

        val good = CoachViewModel(StatsSource { snapshot }, Coach { WeeklyPlan(25, listOf("Coroutines"), "Due cards") }, plans)
        good.propose()
        advanceUntilIdle()
        good.dismiss()
        assertNull(good.uiState.value.proposal)
        assertNull(plans.active.value)
    }
}
