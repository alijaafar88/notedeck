package dev.notedeck.feature.decks

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.notedeck.core.ui.NoteDeckTheme
import dev.notedeck.model.Card
import dev.notedeck.model.Deck

/** Stateful: gets the ViewModel and collects its state. */
@Composable
fun DecksRoute(
    onOpen: (Deck) -> Unit,
    modifier: Modifier = Modifier,
    onBrowseShared: (() -> Unit)? = null,
    viewModel: DecksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DecksScreen(state, onOpen, modifier, onBrowseShared)
}

/** Stateless: draws whatever state it's given. */
@Composable
fun DecksScreen(state: DecksUiState, onOpen: (Deck) -> Unit, modifier: Modifier = Modifier, onBrowseShared: (() -> Unit)? = null) {
    Column(modifier) {
        onBrowseShared?.let { TextButton(onClick = it, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Browse shared decks") } }
        when (state) {
            DecksUiState.Loading -> Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            DecksUiState.Empty -> Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { Text("No decks yet") } }
            is DecksUiState.Content -> DeckListScreen(state.decks, onOpen)
        }
    }
}

@Preview(name = "Content", showBackground = true)
@Preview(name = "Content, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DecksContentPreview() {
    NoteDeckTheme { DecksScreen(DecksUiState.Content(listOf(Deck("Kotlin basics", listOf(Card("1", "val?", "read-only"))))), onOpen = {}, onBrowseShared = {}) }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun DecksEmptyPreview() {
    NoteDeckTheme { DecksScreen(DecksUiState.Empty, onOpen = {}) }
}
