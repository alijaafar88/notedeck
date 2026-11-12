package dev.notedeck.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.notedeck.model.Deck

// TODO(c2): show the decks in a LazyColumn with keys, using MaterialTheme tokens. Add light and dark previews.
@Composable
fun DeckListScreen(decks: List<Deck>, onOpen: (Deck) -> Unit, modifier: Modifier = Modifier) {
}
