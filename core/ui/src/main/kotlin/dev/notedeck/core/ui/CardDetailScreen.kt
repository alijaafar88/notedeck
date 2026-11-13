package dev.notedeck.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.notedeck.model.Card as DeckCard

@Composable
fun CardDetailScreen(card: DeckCard, modifier: Modifier = Modifier) {
    Surface(modifier) {
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(card.front, style = MaterialTheme.typography.titleLarge)
                Text(card.back, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CardDetailPreview() {
    NoteDeckTheme { CardDetailScreen(DeckCard("1", "What makes a type nullable?", "A trailing ?")) }
}
