package dev.notedeck.feature.playground

import dev.notedeck.core.testing.MainDispatcherRule
import dev.notedeck.domain.FakeTextModel
import dev.notedeck.domain.TextModel
import dev.notedeck.domain.TextRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class PlaygroundViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** Streams "<prompt> 1", " 2", " 3", 100 ms apart, and records each request and which runs reached the end. */
    private class RecordingModel : TextModel {
        override val name = "Recording"
        val requests = mutableListOf<TextRequest>()
        val finished = mutableListOf<String>()

        override fun generate(request: TextRequest): Flow<String> = flow {
            requests += request
            for (i in 1..3) {
                delay(100)
                emit(if (i == 1) "${request.prompt} $i" else " $i")
            }
            finished += request.prompt
        }
    }

    @Test
    fun `the output streams in, then the run ends`() = runTest {
        val viewModel = PlaygroundViewModel(RecordingModel())
        viewModel.onPromptChange("A")
        viewModel.run()
        advanceTimeBy(150)
        assertEquals("A 1", viewModel.uiState.value.output)
        assertTrue(viewModel.uiState.value.running)
        advanceUntilIdle()
        assertEquals("A 1 2 3", viewModel.uiState.value.output)
        assertFalse(viewModel.uiState.value.running)
    }

    @Test
    fun `a new run cancels the old one and replaces its output`() = runTest {
        val model = RecordingModel()
        val viewModel = PlaygroundViewModel(model)
        viewModel.onPromptChange("A")
        viewModel.run()
        advanceTimeBy(150)
        viewModel.onPromptChange("B")
        viewModel.run()
        advanceUntilIdle()
        assertEquals("B 1 2 3", viewModel.uiState.value.output)
        assertEquals(listOf("B"), model.finished)
        assertFalse(viewModel.uiState.value.running)
    }

    @Test
    fun `stop cancels the run and keeps what arrived`() = runTest {
        val model = RecordingModel()
        val viewModel = PlaygroundViewModel(model)
        viewModel.onPromptChange("A")
        viewModel.run()
        advanceTimeBy(150)
        viewModel.stop()
        advanceUntilIdle()
        assertEquals("A 1", viewModel.uiState.value.output)
        assertFalse(viewModel.uiState.value.running)
        assertEquals(emptyList(), model.finished)
    }

    @Test
    fun `the temperature reaches the model, kept between 0 and 1`() = runTest {
        val model = RecordingModel()
        val viewModel = PlaygroundViewModel(model)
        viewModel.onPromptChange("A")
        viewModel.onTemperatureChange(1.7f)
        viewModel.run()
        advanceUntilIdle()
        assertEquals(TextRequest("A", temperature = 1f), model.requests.single())
    }

    @Test
    fun `an error is shown and the run ends`() = runTest {
        val failing = object : TextModel {
            override val name = "Failing"
            override fun generate(request: TextRequest): Flow<String> = flow { error("quota exceeded") }
        }
        val viewModel = PlaygroundViewModel(failing)
        viewModel.onPromptChange("A")
        viewModel.run()
        advanceUntilIdle()
        assertEquals("quota exceeded", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.running)
    }

    @Test
    fun `with the fake at temperature 0, the same prompt gives the same output`() = runTest {
        val viewModel = PlaygroundViewModel(FakeTextModel())
        viewModel.onPromptChange("What is a cold flow")
        viewModel.run()
        advanceUntilIdle()
        val first = viewModel.uiState.value.output
        viewModel.run()
        advanceUntilIdle()
        assertEquals(first, viewModel.uiState.value.output)
        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.canRun)
    }
}
