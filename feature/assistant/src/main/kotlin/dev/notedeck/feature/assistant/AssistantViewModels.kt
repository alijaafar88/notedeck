package dev.notedeck.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.domain.AiRating
import dev.notedeck.domain.Answerer
import dev.notedeck.domain.Coach
import dev.notedeck.domain.FeedbackStore
import dev.notedeck.domain.Grounding
import dev.notedeck.domain.NoteIndex
import dev.notedeck.domain.PlanRules
import dev.notedeck.domain.PlanStore
import dev.notedeck.domain.StatsSource
import dev.notedeck.domain.StudySnapshot
import dev.notedeck.domain.WeeklyPlan
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SourceUi(val number: Int, val deckName: String, val text: String, val cited: Boolean = false)

data class AskUiState(
    val question: String = "",
    val asking: Boolean = false,
    val answer: String = "",
    val sources: List<SourceUi> = emptyList(),
    /** Null until an answer is complete; false flags an answer that cites nothing it was given. */
    val grounded: Boolean? = null,
    val error: String? = null,
    val rated: AiRating? = null,
) {
    val canAsk: Boolean get() = question.isNotBlank() && !asking
}

/**
 * Ask my notes: search runs on the phone; only the few passages that match go to the model, numbered, and the
 * answer must cite them. With no matching passage there's no model call at all: the notes don't say.
 */
@HiltViewModel
class AskViewModel @Inject constructor(
    private val notes: NoteIndex,
    private val answerer: Answerer,
    private val feedback: FeedbackStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AskUiState())
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()
    private var job: Job? = null

    fun onQuestionChange(question: String) = _uiState.update { it.copy(question = question) }

    fun ask() {
        val question = _uiState.value.takeIf { it.canAsk }?.question?.trim() ?: return
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(asking = true, answer = "", sources = emptyList(), grounded = null, error = null, rated = null) }
            try {
                val passages = notes.search(question, k = 4).filter { it.score >= Grounding.MIN_SCORE }
                if (passages.isEmpty()) {
                    _uiState.update { it.copy(asking = false, answer = Grounding.NOT_IN_NOTES, grounded = true) }
                    return@launch
                }
                _uiState.update { s -> s.copy(sources = passages.mapIndexed { i, p -> SourceUi(i + 1, p.deckName, p.text) }) }
                answerer.answer(question, passages.map { it.text }).collect { piece -> _uiState.update { it.copy(answer = it.answer + piece) } }
                val check = Grounding.check(_uiState.value.answer, passages.size)
                _uiState.update { s ->
                    s.copy(
                        asking = false,
                        answer = check.text,
                        grounded = check.grounded,
                        sources = s.sources.map { it.copy(cited = it.number in check.cited) },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(asking = false, error = e.message ?: "Couldn't answer. Try again.") }
            }
        }
    }

    fun rate(rating: AiRating) {
        if (_uiState.value.rated != null || _uiState.value.grounded == null) return
        // Marked at once, so a quick second tap can't record twice.
        _uiState.update { it.copy(rated = rating) }
        viewModelScope.launch { feedback.record(feature = "ask", promptVersion = Grounding.PROMPT_VERSION, rating = rating) }
    }
}

data class CoachUiState(
    val snapshot: StudySnapshot? = null,
    val proposing: Boolean = false,
    val proposal: WeeklyPlan? = null,
    val active: WeeklyPlan? = null,
    val error: String? = null,
    val message: String? = null,
)

/**
 * The study coach proposes; the learner decides. The backend's agent reads a snapshot of statistics, never cards
 * or notes. Its plan is validated against the learner's real decks, and changes nothing until Apply.
 */
@HiltViewModel
class CoachViewModel @Inject constructor(
    private val stats: StatsSource,
    private val coach: Coach,
    private val plans: PlanStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { _uiState.update { it.copy(snapshot = stats.snapshot()) } }
        viewModelScope.launch { plans.active.collect { plan -> _uiState.update { it.copy(active = plan) } } }
    }

    fun propose() {
        if (_uiState.value.proposing) return
        viewModelScope.launch {
            _uiState.update { it.copy(proposing = true, proposal = null, error = null, message = null) }
            try {
                val snapshot = stats.snapshot()
                val plan = PlanRules.validate(coach.propose(snapshot), snapshot)
                _uiState.update { it.copy(proposing = false, snapshot = snapshot, proposal = plan) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(proposing = false, error = e.message ?: "The coach isn't available. Try again later.") }
            }
        }
    }

    fun apply() {
        val plan = _uiState.value.proposal ?: return
        viewModelScope.launch {
            plans.apply(plan)
            _uiState.update { it.copy(proposal = null, message = "Plan applied: ${plan.dailyGoal} cards a day.") }
        }
    }

    fun dismiss() = _uiState.update { it.copy(proposal = null) }
}
