package dev.notedeck.feature.playground

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.domain.TextModel
import dev.notedeck.domain.TextRequest
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaygroundUiState(
    val modelName: String,
    val prompt: String = "",
    val temperature: Float = 0f,
    val output: String = "",
    val running: Boolean = false,
    val error: String? = null,
) {
    val canRun: Boolean get() = prompt.isNotBlank() && !running
}

/** Debug-only: try prompts and temperatures against whichever [TextModel] the app binds. */
@HiltViewModel
class PlaygroundViewModel @Inject constructor(private val model: TextModel) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaygroundUiState(modelName = model.name))
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    private var current: Job? = null

    fun onPromptChange(prompt: String) = _uiState.update { it.copy(prompt = prompt) }

    fun onTemperatureChange(temperature: Float) = _uiState.update { it.copy(temperature = temperature.coerceIn(0f, 1f)) }

    /** Starts a run. A run already going is cancelled first, and has finished before this one starts. */
    fun run() {
        val request = _uiState.value.let { TextRequest(it.prompt, it.temperature) }
        val previous = current
        current = viewModelScope.launch {
            previous?.cancelAndJoin()
            _uiState.update { it.copy(output = "", running = true, error = null) }
            model.generate(request)
                .onCompletion { _uiState.update { it.copy(running = false) } }
                .catch { error -> _uiState.update { it.copy(error = error.message ?: "The model failed") } }
                .collect { piece -> _uiState.update { it.copy(output = it.output + piece) } }
        }
    }

    fun stop() {
        current?.cancel()
    }
}
