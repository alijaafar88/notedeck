package dev.notedeck.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.notedeck.model.Card
import dev.notedeck.model.Deck

/** One deck, read through the repository like every other screen, so a new card shows up here at once. */
@Composable
fun DeckRoute(
    name: String,
    onStudy: () -> Unit,
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DecksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deck = (state as? DecksUiState.Content)?.decks?.firstOrNull { it.name == name }
    DeckScreen(deck, loading = state == DecksUiState.Loading, onStudy = onStudy, onAddCard = onAddCard, modifier = modifier)
}

@Composable
fun DeckScreen(deck: Deck?, loading: Boolean, onStudy: () -> Unit, onAddCard: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize()) {
        when {
            loading -> Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            deck == null -> Box(contentAlignment = Alignment.Center) { Text("This deck no longer exists") }
            else -> Column {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStudy) { Text("Study this deck") }
                    OutlinedButton(onClick = onAddCard) { Text("Add card") }
                }
                Text("${deck.cards.size} cards", Modifier.padding(horizontal = 16.dp))
                deck.cards.firstOrNull()?.let { CardDetailScreen(it) } ?: Text("No cards yet", Modifier.padding(16.dp))
            }
        }
    }
}

@Preview(name = "Deck", showBackground = true)
@Preview(name = "Deck, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeckPreview() {
    NoteDeckTheme { DeckScreen(Deck("Kotlin basics", listOf(Card("1", "val or var?", "val"))), loading = false, onStudy = {}, onAddCard = {}) }
}

@Preview(name = "Empty deck", showBackground = true)
@Composable
private fun EmptyDeckPreview() {
    NoteDeckTheme { DeckScreen(Deck("Coroutines", emptyList()), loading = false, onStudy = {}, onAddCard = {}) }
}
