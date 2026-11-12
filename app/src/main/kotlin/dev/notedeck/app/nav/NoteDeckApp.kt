package dev.notedeck.app.nav

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import dev.notedeck.app.SampleDecks
import dev.notedeck.app.ui.CardDetailScreen
import dev.notedeck.app.ui.DeckListScreen

private val labels = listOf("Decks", "Study", "Settings")

@Composable
fun NoteDeckApp() {
    val stacks: List<NavBackStack<NavKey>> = listOf(rememberNavBackStack(DecksKey), rememberNavBackStack(StudyKey()), rememberNavBackStack(SettingsKey))
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val current = stacks[selected]
    val wide = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(bottomBar = {
        NavigationBar {
            labels.forEachIndexed { index, label ->
                NavigationBarItem(selected = selected == index, onClick = { selected = index }, icon = {}, label = { Text(label) })
            }
        }
    }) { padding ->
        NavDisplay(
            backStack = current,
            modifier = Modifier.padding(padding),
            onBack = { if (current.size > 1) current.removeLastOrNull() else selected = 0 },
            entryProvider = entryProvider {
                entry<DecksKey> { DeckListScreen(SampleDecks.all, onOpen = { current.add(DeckKey(it.name)) }) }
                entry<DeckKey> { key ->
                    val deck = SampleDecks.all.first { it.name == key.name }
                    val detail = @Composable { deck.cards.firstOrNull()?.let { CardDetailScreen(it) } ?: Text("No cards yet") }
                    if (wide) {
                        // List-detail on tablets and unfolded foldables.
                        Row {
                            DeckListScreen(SampleDecks.all, onOpen = { current[current.lastIndex] = DeckKey(it.name) }, Modifier.weight(1f))
                            Row(Modifier.weight(2f)) { detail() }
                        }
                    } else {
                        detail()
                    }
                }
                entry<StudyKey> { key -> Text(key.deckName?.let { "Studying $it" } ?: "Pick a deck to study") }
                entry<SettingsKey> { Text("Settings") }
            },
        )
    }
}
