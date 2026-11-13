package dev.notedeck.feature.assistant

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.notedeck.core.ui.NoteDeckTheme
import dev.notedeck.domain.DeckStats
import dev.notedeck.domain.StudySnapshot
import dev.notedeck.domain.WeeklyPlan

@Composable
fun AskRoute(modifier: Modifier = Modifier, viewModel: AskViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AskScreen(state, viewModel::onQuestionChange, viewModel::ask, modifier)
}

@Composable
fun AskScreen(state: AskUiState, onQuestionChange: (String) -> Unit, onAsk: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ask my notes", style = MaterialTheme.typography.titleLarge)
            Text(
                "Your notes are searched on this phone. Only the passages that match are sent to NoteDeck's server to write the answer.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(state.question, onQuestionChange, label = { Text("Your question") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = onAsk, enabled = state.canAsk) { Text(if (state.asking) "Answering..." else "Ask") }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.answer.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AI answer, from your notes", style = MaterialTheme.typography.labelMedium)
                        Text(state.answer, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
            }
            if (state.grounded == false) {
                Text("This answer doesn't cite your notes. Check it before you trust it.", color = MaterialTheme.colorScheme.error)
            }
            if (state.sources.isNotEmpty()) {
                Text("Sources", style = MaterialTheme.typography.titleMedium)
                state.sources.forEach { source ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            val label = "[${source.number}] ${source.deckName}" + if (source.cited) ", cited" else ""
                            Text(label, style = MaterialTheme.typography.labelMedium)
                            Text(source.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachRoute(modifier: Modifier = Modifier, viewModel: CoachViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CoachScreen(state, viewModel::propose, viewModel::apply, viewModel::dismiss, modifier)
}

@Composable
fun CoachScreen(state: CoachUiState, onPropose: () -> Unit, onApply: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Study coach", style = MaterialTheme.typography.titleLarge)
            state.snapshot?.let { s ->
                Text("Daily goal: ${s.dailyGoal} cards. Due now: ${s.decks.sumOf { it.dueNow }} cards in ${s.decks.size} decks.")
            }
            Text(
                "The coach reads your review statistics (not your cards or notes) on NoteDeck's server, and proposes a plan. Nothing changes until you apply it.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onPropose, enabled = !state.proposing) { Text(if (state.proposing) "Thinking..." else "Propose a plan for next week") }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it) }
            state.proposal?.let { plan ->
                PlanCard("Proposed plan", plan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApply) { Text("Apply") }
                    OutlinedButton(onClick = onDismiss) { Text("Not now") }
                }
            }
            state.active?.let { PlanCard("Your current plan", it) }
        }
    }
}

@Composable
private fun PlanCard(title: String, plan: WeeklyPlan) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text("Daily goal: ${plan.dailyGoal} cards")
            Text("Focus: ${plan.focusDecks.joinToString(", ")}")
            Text(plan.reason, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(name = "Answer with sources", showBackground = true)
@Preview(name = "Answer with sources, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AskPreview() = NoteDeckTheme {
    AskScreen(
        AskUiState(
            question = "What is a channel?",
            answer = "A hot pipe between coroutines [1].",
            grounded = true,
            sources = listOf(SourceUi(1, "Coroutines", "Channel: a hot pipe between coroutines", cited = true)),
        ),
        onQuestionChange = {},
        onAsk = {},
    )
}

@Preview(name = "Ungrounded answer", showBackground = true)
@Composable
private fun AskUngroundedPreview() = NoteDeckTheme {
    AskScreen(AskUiState(question = "Q", answer = "Channels are hot.", grounded = false), onQuestionChange = {}, onAsk = {})
}

@Preview(name = "Proposed plan", showBackground = true)
@Composable
private fun CoachPreview() = NoteDeckTheme {
    CoachScreen(
        CoachUiState(
            snapshot = StudySnapshot(20, listOf(DeckStats("Coroutines", 6, 5, 2, 0)), List(7) { 3 }),
            proposal = WeeklyPlan(25, listOf("Coroutines"), "Five cards are due there, and you reviewed it least."),
        ),
        onPropose = {},
        onApply = {},
        onDismiss = {},
    )
}
