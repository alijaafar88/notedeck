package dev.notedeck.app.nav

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.notedeck.app.ui.AddCardRoute
import dev.notedeck.app.ui.DeckRoute
import dev.notedeck.app.ui.DecksRoute
import dev.notedeck.app.ui.DecksViewModel
import dev.notedeck.app.ui.SettingsRoute
import dev.notedeck.app.ui.SharedDecksRoute
import dev.notedeck.app.ui.StudyRoute

private val labels = listOf("Decks", "Study", "Settings")

@Composable
fun NoteDeckApp() {
    val stacks: List<NavBackStack<NavKey>> = listOf(rememberNavBackStack(DecksKey), rememberNavBackStack(StudyKey()), rememberNavBackStack(SettingsKey))
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val current = stacks[selected]
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

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
            // Each entry gets its own ViewModelStore, so a screen's ViewModel is cleared when it's popped.
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
            entryProvider = entryProvider {
                entry<DecksKey> { DecksRoute(onOpen = { current.add(DeckKey(it.name)) }, onBrowseShared = { current.add(SharedDecksKey) }) }
                entry<SharedDecksKey> { SharedDecksRoute() }
                entry<DeckKey> { key ->
                    val detail = @Composable {
                        DeckRoute(
                            key.name,
                            onStudy = { stacks[1].add(StudyKey(key.name)); selected = 1 },
                            onAddCard = { current.add(AddCardKey(key.name)) },
                        )
                    }
                    if (wide) {
                        // List-detail on tablets and unfolded foldables.
                        Row {
                            DecksRoute(onOpen = { current[current.lastIndex] = DeckKey(it.name) }, Modifier.weight(1f))
                            Row(Modifier.weight(2f)) { detail() }
                        }
                    } else {
                        detail()
                    }
                }
                entry<AddCardKey> { key ->
                    val viewModel: DecksViewModel = hiltViewModel()
                    AddCardRoute(onSaved = { front, back -> viewModel.addCard(key.deckName, front, back) })
                }
                entry<StudyKey> { key ->
                    StudyRoute(key.deckName, onClose = {
                        if (current.size > 1) current.removeLastOrNull()
                        selected = 0
                    })
                }
                entry<SettingsKey> { SettingsRoute() }
            },
        )
    }
}
