package dev.notedeck.feature.generate

import dev.notedeck.core.testing.MainDispatcherRule
import dev.notedeck.domain.CardDraft
import dev.notedeck.domain.CardGenerator
import dev.notedeck.domain.CardGenerators
import dev.notedeck.domain.DraftEvent
import dev.notedeck.domain.FakeCardGenerator
import dev.notedeck.domain.FakeDeckRepository
import dev.notedeck.domain.InvalidReplyException
import dev.notedeck.domain.NotesPhoto
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class GenerateViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val deck = Deck("Coroutines", listOf(Card("1", "What is Flow?", "A cold stream")))

    /** Streams two pieces 100 ms apart, then the drafts, and records what it was asked. */
    private class Scripted(
        override val id: String = "scripted",
        override val available: Boolean = true,
        override val acceptsPhotos: Boolean = true,
        private val drafts: List<CardDraft> = emptyList(),
        private val failure: Exception? = null,
    ) : CardGenerator {
        override val name = id
        val photos = mutableListOf<NotesPhoto?>()

        override fun draft(notes: String, photo: NotesPhoto?): Flow<DraftEvent> = flow {
            photos += photo
            delay(100)
            emit(DraftEvent.Text("[{\"front\""))
            delay(100)
            failure?.let { throw it }
            emit(DraftEvent.Text(": ...}]"))
            emit(DraftEvent.Done(drafts))
        }
    }

    private fun viewModel(vararg generators: CardGenerator, repository: FakeDeckRepository = FakeDeckRepository(listOf(deck))) =
        GenerateViewModel("Coroutines", CardGenerators(generators.toList()), repository)

    @Test
    fun `unavailable providers are listed but the first available one is chosen`() {
        val state = viewModel(Scripted("gemini", available = false), Scripted("claude")).uiState.value
        assertEquals(listOf("gemini", "claude"), state.providers.map { it.id })
        assertEquals("claude", state.providerId)
        assertFalse(state.canGenerate, "no notes yet")
    }

    @Test
    fun `an unavailable provider can't be chosen`() {
        val viewModel = viewModel(Scripted("gemini", available = false), Scripted("claude"))
        viewModel.chooseProvider("gemini")
        assertEquals("claude", viewModel.uiState.value.providerId)
    }

    @Test
    fun `text streams in, then the cleaned drafts are shown for review`() = runTest {
        val drafts = listOf(CardDraft("what is flow?", "duplicate of the deck's card"), CardDraft(" Is a Flow cold? ", "Yes"), CardDraft("", "blank"))
        val viewModel = viewModel(Scripted(drafts = drafts))
        viewModel.onNotesChange("Flows are cold")
        viewModel.generate()
        advanceTimeBy(150)
        assertEquals(GeneratePhase.Streaming, viewModel.uiState.value.phase)
        assertEquals("[{\"front\"", viewModel.uiState.value.streamed)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(GeneratePhase.Reviewing, state.phase)
        assertEquals(listOf(EditableCard(0, "Is a Flow cold?", "Yes")), state.cards)
    }

    @Test
    fun `only the cards the learner kept and edited are saved`() = runTest {
        val repository = FakeDeckRepository(listOf(deck))
        val viewModel = viewModel(FakeCardGenerator(), repository = repository)
        viewModel.onNotesChange("Channel: a hot pipe\nJob: a handle to a coroutine\nscope - where coroutines live")
        viewModel.generate()
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.cards.size)
        viewModel.editBack(0, "A hot pipe between coroutines")
        viewModel.remove(1)
        viewModel.save()
        advanceUntilIdle()
        assertEquals(GeneratePhase.Saved, viewModel.uiState.value.phase)
        assertEquals(2, viewModel.uiState.value.savedCount)
        val saved = repository.decks.first().single().cards.drop(1).map { it.front to it.back }
        assertEquals(listOf("What is Channel?" to "A hot pipe between coroutines", "What is scope?" to "where coroutines live"), saved)
    }

    @Test
    fun `a card with an empty side can't be saved`() = runTest {
        val viewModel = viewModel(Scripted(drafts = listOf(CardDraft("Q", "A"))))
        viewModel.onNotesChange("notes")
        viewModel.generate()
        advanceUntilIdle()
        viewModel.editBack(0, " ")
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `a failure shows its message and keeps the notes for a retry`() = runTest {
        val viewModel = viewModel(Scripted(failure = InvalidReplyException("The model's reply was cut off or malformed. Try again.")))
        viewModel.onNotesChange("notes")
        viewModel.generate()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(GeneratePhase.Writing, state.phase)
        assertEquals("The model's reply was cut off or malformed. Try again.", state.error)
        assertEquals("notes", state.notes)
        assertTrue(state.canGenerate)
    }

    @Test
    fun `no usable cards is explained, not shown as an empty review`() = runTest {
        val viewModel = viewModel(Scripted(drafts = listOf(CardDraft("What is Flow?", "dup"))))
        viewModel.onNotesChange("notes")
        viewModel.generate()
        advanceUntilIdle()
        assertEquals(GeneratePhase.Writing, viewModel.uiState.value.phase)
        assertEquals("No new cards in these notes.", viewModel.uiState.value.error)
    }

    @Test
    fun `stop cancels the stream and keeps what arrived`() = runTest {
        val viewModel = viewModel(Scripted(drafts = listOf(CardDraft("Q", "A"))))
        viewModel.onNotesChange("notes")
        viewModel.generate()
        advanceTimeBy(150)
        viewModel.stop()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(GeneratePhase.Writing, state.phase)
        assertEquals("[{\"front\"", state.streamed)
        assertNull(state.error)
        assertEquals(emptyList(), state.cards)
    }

    @Test
    fun `a photo goes only to a provider that accepts photos`() = runTest {
        val gemini = Scripted("gemini", acceptsPhotos = true, drafts = listOf(CardDraft("Q", "A")))
        val claude = Scripted("claude", acceptsPhotos = false, drafts = listOf(CardDraft("Q", "A")))
        val viewModel = viewModel(gemini, claude)
        val photo = NotesPhoto(byteArrayOf(1, 2, 3))
        viewModel.onPhoto(photo)
        assertTrue(viewModel.uiState.value.canGenerate, "a photo alone is enough")
        viewModel.generate()
        advanceUntilIdle()
        assertEquals(listOf<NotesPhoto?>(photo), gemini.photos)

        viewModel.startOver()
        viewModel.chooseProvider("claude")
        assertFalse(viewModel.uiState.value.hasPhoto)
        viewModel.onNotesChange("notes")
        viewModel.generate()
        advanceUntilIdle()
        assertEquals(listOf<NotesPhoto?>(null), claude.photos)
    }
}
