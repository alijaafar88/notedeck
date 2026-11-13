package dev.notedeck.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.app.data.Settings
import dev.notedeck.app.data.SettingsRepository
import dev.notedeck.app.data.ThemeChoice
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    /** Null until DataStore has been read, so the screen never flashes the defaults. */
    val settings: StateFlow<Settings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun changeGoal(by: Int) {
        val current = settings.value ?: return
        viewModelScope.launch { repository.setDailyGoal(current.dailyGoal + by) }
    }

    fun chooseTheme(theme: ThemeChoice) {
        viewModelScope.launch { repository.setTheme(theme) }
    }
}

@Composable
fun SettingsRoute(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    settings?.let { SettingsScreen(it, onChangeGoal = viewModel::changeGoal, onChooseTheme = viewModel::chooseTheme, modifier = modifier) }
}

@Composable
fun SettingsScreen(settings: Settings, onChangeGoal: (Int) -> Unit, onChooseTheme: (ThemeChoice) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Daily goal", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onChangeGoal(-5) }, enabled = settings.dailyGoal > Settings.GOAL_RANGE.first) { Text("Fewer") }
                Text("${settings.dailyGoal} cards a day")
                OutlinedButton(onClick = { onChangeGoal(5) }, enabled = settings.dailyGoal < Settings.GOAL_RANGE.last) { Text("More") }
            }
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            ThemeChoice.entries.forEach { choice ->
                Row(
                    Modifier.fillMaxWidth().selectable(selected = settings.theme == choice, onClick = { onChooseTheme(choice) }, role = Role.RadioButton),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.theme == choice, onClick = null)
                    Text(choice.label, Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private val ThemeChoice.label: String
    get() = when (this) {
        ThemeChoice.SYSTEM -> "Follow the system"
        ThemeChoice.LIGHT -> "Light"
        ThemeChoice.DARK -> "Dark"
    }

@Preview(name = "Settings", showBackground = true)
@Preview(name = "Settings, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreview() {
    NoteDeckTheme { SettingsScreen(Settings(dailyGoal = 25, theme = ThemeChoice.DARK), onChangeGoal = {}, onChooseTheme = {}) }
}
