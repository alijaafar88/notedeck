package dev.notedeck.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.app.data.SharedDeckRepository
import dev.notedeck.app.network.NetworkMonitor
import dev.notedeck.app.network.NetworkResult
import dev.notedeck.domain.DeckRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SharedDeckRow(val id: String, val name: String, val cardCount: Int, val imported: Boolean)

data class SharedDecksUiState(
    val decks: List<SharedDeckRow> = emptyList(),
    val offline: Boolean = false,
    val loading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class SharedDecksViewModel @Inject constructor(
    private val shared: SharedDeckRepository,
    decks: DeckRepository,
    monitor: NetworkMonitor,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val refreshing = MutableStateFlow(true)

    /** Everything on screen comes from Room (catalog and own decks) plus the connection state. */
    val uiState: StateFlow<SharedDecksUiState> = combine(shared.catalog, decks.decks, monitor.isOnline, message, refreshing) { catalog, mine, online, note, busy ->
        val owned = mine.map { it.name }.toSet()
        SharedDecksUiState(
            decks = catalog.map { SharedDeckRow(it.id, it.name, it.cardCount, imported = it.name in owned) },
            offline = !online,
            loading = busy && catalog.isEmpty(),
            message = note,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SharedDecksUiState())

    init {
        viewModelScope.launch {
            report(shared.refreshIfStale())
            refreshing.value = false
        }
    }

    fun retry() {
        viewModelScope.launch {
            refreshing.value = true
            report(shared.refresh())
            refreshing.value = false
        }
    }

    fun import(id: String) {
        viewModelScope.launch { report(shared.import(id)) }
    }

    /** Offline needs no message: the banner says it, over whatever is cached. */
    private fun report(result: NetworkResult<Unit>) {
        message.value = when (result) {
            is NetworkResult.Success, NetworkResult.Offline -> null
            is NetworkResult.HttpError -> "The server answered ${result.code}. Try again later."
        }
    }
}

@Composable
fun SharedDecksRoute(modifier: Modifier = Modifier, viewModel: SharedDecksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SharedDecksScreen(state, onImport = viewModel::import, onRetry = viewModel::retry, modifier = modifier)
}

@Composable
fun SharedDecksScreen(state: SharedDecksUiState, onImport: (String) -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize()) {
        Column {
            if (state.offline) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text("You're offline. Showing the decks saved last time.", Modifier.padding(12.dp))
                }
            }
            state.message?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            when {
                state.decks.isNotEmpty() -> LazyColumn {
                    items(state.decks, key = { it.id }) { deck -> SharedDeckItem(deck, onImport) }
                }
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No shared decks yet")
                    Button(onClick = onRetry) { Text("Try again") }
                }
            }
        }
    }
}

@Composable
private fun SharedDeckItem(deck: SharedDeckRow, onImport: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(deck.name, style = MaterialTheme.typography.titleMedium)
            Text("${deck.cardCount} cards", style = MaterialTheme.typography.bodySmall)
        }
        if (deck.imported) Text("Imported") else Button(onClick = { onImport(deck.id) }) { Text("Import") }
    }
}

private val previewDecks = listOf(
    SharedDeckRow("kotlin-collections", "Kotlin collections", 4, imported = true),
    SharedDeckRow("compose-state", "Compose state", 4, imported = false),
)

@Preview(name = "Online", showBackground = true)
@Preview(name = "Online, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SharedDecksPreview() {
    NoteDeckTheme { SharedDecksScreen(SharedDecksUiState(previewDecks, loading = false), onImport = {}, onRetry = {}) }
}

@Preview(name = "Offline, cached", showBackground = true)
@Composable
private fun SharedDecksOfflinePreview() {
    NoteDeckTheme { SharedDecksScreen(SharedDecksUiState(previewDecks, offline = true, loading = false), onImport = {}, onRetry = {}) }
}

@Preview(name = "Nothing cached", showBackground = true)
@Composable
private fun SharedDecksEmptyPreview() {
    NoteDeckTheme { SharedDecksScreen(SharedDecksUiState(loading = false, offline = true), onImport = {}, onRetry = {}) }
}
