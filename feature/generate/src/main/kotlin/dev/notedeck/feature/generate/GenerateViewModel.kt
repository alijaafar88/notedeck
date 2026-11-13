package dev.notedeck.feature.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.domain.CardGenerators
import dev.notedeck.domain.CardRules
import dev.notedeck.domain.DeckRepository
import dev.notedeck.domain.DraftEvent
import dev.notedeck.domain.NoteIndex
import dev.notedeck.domain.NotesPhoto
import dev.notedeck.domain.OcrScanner
import dev.notedeck.domain.Summary
import dev.notedeck.domain.SummaryRoute
import dev.notedeck.domain.SummaryRouter
import dev.notedeck.model.Card
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderChoice(val id: String, val name: String, val available: Boolean, val acceptsPhotos: Boolean)

data class EditableCard(val key: Int, val front: String, val back: String) {
    val complete: Boolean get() = front.isNotBlank() && back.isNotBlank()
}

enum class GeneratePhase { Writing, Streaming, Reviewing, Saved }

data class GenerateUiState(
    val deckName: String,
    val providers: List<ProviderChoice>,
    val providerId: String?,
    val notes: String = "",
    val hasPhoto: Boolean = false,
    val phase: GeneratePhase = GeneratePhase.Writing,
    val streamed: String = "",
    val cards: List<EditableCard> = emptyList(),
    val error: String? = null,
    val savedCount: Int = 0,
    val summaryRoute: SummaryRoute? = null,
    val summarizing: Boolean = false,
    val summary: Summary? = null,
    val scanning: Boolean = false,
) {
    val provider: ProviderChoice? get() = providers.firstOrNull { it.id == providerId }
    val canGenerate: Boolean get() = phase == GeneratePhase.Writing && provider?.available == true && (notes.isNotBlank() || hasPhoto)
    val canSave: Boolean get() = phase == GeneratePhase.Reviewing && cards.any { it.complete }
    val canSummarize: Boolean
        get() = notes.isNotBlank() && !summarizing && phase == GeneratePhase.Writing &&
            (summaryRoute == SummaryRoute.OnDevice || summaryRoute == SummaryRoute.Cloud)
}

/**
 * Notes (and maybe a photo) in, reviewed cards out. The model's drafts are only suggestions: nothing reaches
 * the deck until the learner has seen, edited and saved them.
 */
@HiltViewModel(assistedFactory = GenerateViewModel.Factory::class)
class GenerateViewModel @AssistedInject constructor(
    @Assisted private val deckName: String,
    private val generators: CardGenerators,
    private val repository: DeckRepository,
    private val summaries: SummaryRouter,
    private val ocr: OcrScanner,
    private val noteIndex: NoteIndex,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GenerateUiState(
            deckName = deckName,
            providers = generators.all.map { ProviderChoice(it.id, it.name, it.available, it.acceptsPhotos) },
            providerId = generators.all.firstOrNull { it.available }?.id,
        ),
    )
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    private var photo: NotesPhoto? = null
    private var job: Job? = null

    init {
        viewModelScope.launch { refreshSummaryRoute() }
    }

    /** Asked again after each summary or download, and whenever the screen opens, so a Settings change shows up. */
    private suspend fun refreshSummaryRoute() {
        val route = summaries.route()
        _uiState.update { it.copy(summaryRoute = route) }
    }

    fun summarize() {
        val notes = _uiState.value.takeIf { it.canSummarize }?.notes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(summarizing = true, summary = null, error = null) }
            try {
                val summary = summaries.summarize(notes)
                _uiState.update { it.copy(summarizing = false, summary = summary) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(summarizing = false, error = e.message ?: "Couldn't summarise these notes. Try again.") }
            }
            refreshSummaryRoute()
        }
    }

    fun downloadSummaries() {
        viewModelScope.launch {
            _uiState.update { it.copy(summaryRoute = SummaryRoute.Downloading, error = null) }
            try {
                summaries.download()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "The download didn't finish. Try again on Wi-Fi.") }
            }
            refreshSummaryRoute()
        }
    }

    /** Reads a photo's text on the phone and adds it to the notes. The photo itself goes nowhere. */
    fun scan(picked: NotesPhoto?) {
        if (picked == null) {
            _uiState.update { it.copy(error = "Couldn't open that photo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(scanning = true, error = null) }
            val text = try {
                ocr.read(picked)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _uiState.update { state ->
                if (text == null) {
                    state.copy(scanning = false, error = "No text found in that photo.")
                } else {
                    state.copy(scanning = false, notes = listOf(state.notes.trim(), text).filter { it.isNotEmpty() }.joinToString("\n"))
                }
            }
        }
    }

    fun onNotesChange(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun chooseProvider(id: String) = _uiState.update { state ->
        val choice = state.providers.firstOrNull { it.id == id && it.available } ?: return@update state
        if (!choice.acceptsPhotos) photo = null
        state.copy(providerId = id, hasPhoto = state.hasPhoto && choice.acceptsPhotos)
    }

    fun onPhoto(picked: NotesPhoto?) {
        photo = picked
        _uiState.update { it.copy(hasPhoto = picked != null) }
    }

    fun generate() {
        val state = _uiState.value
        if (!state.canGenerate) return
        val generator = generators.all.first { it.id == state.providerId }
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(phase = GeneratePhase.Streaming, streamed = "", cards = emptyList(), error = null) }
            try {
                generator.draft(state.notes, photo).collect { event ->
                    when (event) {
                        is DraftEvent.Text -> _uiState.update { it.copy(streamed = it.streamed + event.piece) }
                        is DraftEvent.Done -> review(event)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(phase = GeneratePhase.Writing, error = e.message ?: "Generating cards failed. Try again.") }
            }
        }
    }

    private suspend fun review(done: DraftEvent.Done) {
        val existing = repository.decks.first().firstOrNull { it.name == deckName }?.cards?.map { it.front }.orEmpty()
        val drafts = CardRules.clean(done.drafts, existing)
        _uiState.update {
            if (drafts.isEmpty()) {
                it.copy(phase = GeneratePhase.Writing, error = "No new cards in these notes.")
            } else {
                it.copy(phase = GeneratePhase.Reviewing, cards = drafts.mapIndexed { i, d -> EditableCard(i, d.front, d.back) })
            }
        }
    }

    fun stop() {
        job?.cancel()
        _uiState.update { if (it.phase == GeneratePhase.Streaming) it.copy(phase = GeneratePhase.Writing) else it }
    }

    fun editFront(key: Int, front: String) = editCard(key) { it.copy(front = front) }

    fun editBack(key: Int, back: String) = editCard(key) { it.copy(back = back) }

    fun remove(key: Int) = _uiState.update { state ->
        val cards = state.cards.filterNot { it.key == key }
        state.copy(cards = cards, phase = if (cards.isEmpty()) GeneratePhase.Writing else state.phase)
    }

    private fun editCard(key: Int, change: (EditableCard) -> EditableCard) =
        _uiState.update { state -> state.copy(cards = state.cards.map { if (it.key == key) change(it) else it }) }

    /** Saves the complete cards the learner kept. Their edits are final; nothing goes back to the model. */
    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val keep = state.cards.filter { it.complete }
        viewModelScope.launch {
            keep.forEach { repository.addCard(deckName, Card(UUID.randomUUID().toString(), it.front.trim(), it.back.trim())) }
            // Keep the notes the cards came from, chunked and embedded, so Ask can answer from them later.
            if (state.notes.isNotBlank()) noteIndex.add(deckName, state.notes)
            _uiState.update { it.copy(phase = GeneratePhase.Saved, savedCount = keep.size, cards = emptyList()) }
        }
    }

    fun startOver() = _uiState.update { it.copy(phase = GeneratePhase.Writing, streamed = "", cards = emptyList(), error = null) }

    @AssistedFactory
    interface Factory {
        fun create(deckName: String): GenerateViewModel
    }
}
