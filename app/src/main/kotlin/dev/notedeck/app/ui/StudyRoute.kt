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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StudyRoute(deckName: String?, modifier: Modifier = Modifier, viewModel: StudyViewModel = viewModel(factory = StudyViewModel.factory(deckName))) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StudyScreen(state, onReveal = viewModel::reveal, onAnswer = viewModel::answer, modifier = modifier)
}

@Composable
fun StudyScreen(state: StudyUiState, onReveal: () -> Unit, onAnswer: (knewIt: Boolean) -> Unit, modifier: Modifier = Modifier) {
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
                            OutlinedButton(onClick = { onAnswer(false) }) { Text("Missed it") }
                            Button(onClick = { onAnswer(true) }) { Text("Knew it") }
                        }
                    } else {
                        Button(onClick = onReveal) { Text("Show answer") }
                    }
                }
                is StudyUiState.Finished -> Text("Done: you knew ${state.known} of ${state.total}", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Preview(name = "Question", showBackground = true)
@Preview(name = "Question, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StudyQuestionPreview() {
    NoteDeckTheme { StudyScreen(StudyUiState.Question("What does launch return?", "A Job", revealed = true, position = 1, total = 3), {}, {}) }
}

@Preview(name = "Finished", showBackground = true)
@Composable
private fun StudyFinishedPreview() {
    NoteDeckTheme { StudyScreen(StudyUiState.Finished(known = 2, total = 3), {}, {}) }
}
