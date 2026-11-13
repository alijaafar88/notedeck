package dev.notedeck.feature.decks

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.notedeck.core.ui.NoteDeckTheme
import dev.notedeck.core.ui.cardCount
import dev.notedeck.model.Card as DeckCard
import dev.notedeck.model.Deck

@Composable
fun DeckListScreen(decks: List<Deck>, onOpen: (Deck) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(decks, key = { it.name }) { deck ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onOpen(deck) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(deck.name, style = MaterialTheme.typography.titleMedium)
                        Text(cardCount(deck.cards.size), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private val previewDecks = listOf(Deck("Kotlin basics", listOf(DeckCard("1", "val?", "read-only"))), Deck("Compose", emptyList()))

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeckListPreview() {
    NoteDeckTheme { DeckListScreen(previewDecks, onOpen = {}) }
}
