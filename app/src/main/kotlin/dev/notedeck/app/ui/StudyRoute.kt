package dev.notedeck.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StudyRoute(
    deckName: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudyViewModel = viewModel(factory = StudyViewModel.factory(deckName)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val close by rememberUpdatedState(onClose)
    // One-off effects: each is received once, even if it was sent during a rotation.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                StudyEffect.Close -> close()
            }
        }
    }
    StudyScreen(state, onIntent = viewModel::dispatch, modifier = modifier)
}

@Composable
fun StudyScreen(state: StudyUiState, onIntent: (StudyIntent) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state) {
                StudyUiState.Loading -> CircularProgressIndicator()
                StudyUiState.NoDeck -> Text("Pick a deck to study")
                is StudyUiState.Question -> {
                    Text("Card ${state.position} of ${state.total}", style = MaterialTheme.typography.labelLarge)
                    Text(state.front, style = MaterialTheme.typography.headlineSmall)
                    if (state.revealed) {
                        Text(state.back, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onIntent(StudyIntent.Answer(knewIt = false)) }) { Text("Missed it") }
                            Button(onClick = { onIntent(StudyIntent.Answer(knewIt = true)) }) { Text("Knew it") }
                        }
                    } else {
                        Button(onClick = { onIntent(StudyIntent.Reveal) }) { Text("Show answer") }
                    }
                }
                is StudyUiState.Finished -> {
                    Text("Done: you knew ${state.known} of ${state.total}", style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onIntent(StudyIntent.Restart) }) { Text("Study again") }
                        Button(onClick = { onIntent(StudyIntent.Done) }) { Text("Done") }
                    }
                }
            }
        }
    }
}

@Preview(name = "Question", showBackground = true)
@Preview(name = "Question, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StudyQuestionPreview() {
    NoteDeckTheme { StudyScreen(StudyUiState.Question("What does launch return?", "A Job", revealed = true, position = 1, total = 3), {}) }
}

@Preview(name = "Finished", showBackground = true)
@Composable
private fun StudyFinishedPreview() {
    NoteDeckTheme { StudyScreen(StudyUiState.Finished(known = 2, total = 3), {}) }
}
