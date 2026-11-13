package dev.notedeck.feature.playground

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.notedeck.core.ui.NoteDeckTheme
import kotlin.math.roundToInt

@Composable
fun PlaygroundRoute(modifier: Modifier = Modifier, viewModel: PlaygroundViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlaygroundScreen(
        state,
        onPromptChange = viewModel::onPromptChange,
        onTemperatureChange = viewModel::onTemperatureChange,
        onRun = viewModel::run,
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

@Composable
fun PlaygroundScreen(
    state: PlaygroundUiState,
    onPromptChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier) {
        Column(
            Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Prompt Playground", style = MaterialTheme.typography.titleLarge)
            Text("Model: ${state.modelName}. Debug builds only.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = state.prompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Temperature: ${(state.temperature * 10).roundToInt() / 10.0}")
            Slider(
                value = state.temperature,
                onValueChange = onTemperatureChange,
                valueRange = 0f..1f,
                steps = 9,
                modifier = Modifier.semantics { contentDescription = "Temperature" },
            )
            if (state.running) {
                OutlinedButton(onClick = onStop) { Text("Stop") }
            } else {
                Button(onClick = onRun, enabled = state.canRun) { Text("Run") }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.output.isNotEmpty()) {
                // A polite live region: screen readers read the reply as it streams in, without interrupting.
                Text(state.output, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}

@Preview(name = "Streaming", showBackground = true)
@Preview(name = "Streaming, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaygroundStreamingPreview() {
    NoteDeckTheme {
        PlaygroundScreen(
            PlaygroundUiState(modelName = "Fake", prompt = "Make a card about cold flows", temperature = 0.3f, output = "Here's a card. Front:", running = true),
            onPromptChange = {}, onTemperatureChange = {}, onRun = {}, onStop = {},
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun PlaygroundErrorPreview() {
    NoteDeckTheme {
        PlaygroundScreen(
            PlaygroundUiState(modelName = "Fake", prompt = "Q", error = "The model failed"),
            onPromptChange = {}, onTemperatureChange = {}, onRun = {}, onStop = {},
        )
    }
}
