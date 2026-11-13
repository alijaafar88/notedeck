package dev.notedeck.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notedeck.app.BuildConfig
import dev.notedeck.core.data.Settings
import dev.notedeck.core.data.SettingsRepository
import dev.notedeck.core.data.ThemeChoice
import dev.notedeck.core.data.work.ReviewReminderWorker
import dev.notedeck.core.ui.NoteDeckTheme
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val workManager: WorkManager,
) : ViewModel() {
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

    fun setReminders(on: Boolean) {
        viewModelScope.launch { repository.setReminders(on) }
    }

    /** Debug builds only. Forcing the periodic job doesn't work: WorkManager skips periodic work that runs before its schedule. */
    fun sendTestReminder() {
        workManager.enqueue(OneTimeWorkRequestBuilder<ReviewReminderWorker>().build())
    }
}

/** Turning reminders on is the moment to ask for the notification permission (Android 13+). A no leaves them off. */
@Composable
fun SettingsRoute(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    val askForPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        viewModel.setReminders(granted)
    }
    val onRemindersChange: (Boolean) -> Unit = { on ->
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (on && needsPermission) {
            askForPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            denied = false
            viewModel.setReminders(on)
        }
    }
    settings?.let {
        SettingsScreen(
            it,
            onChangeGoal = viewModel::changeGoal,
            onChooseTheme = viewModel::chooseTheme,
            onRemindersChange = onRemindersChange,
            remindersDenied = denied,
            onTestReminder = if (BuildConfig.DEBUG && it.reminders) viewModel::sendTestReminder else null,
            modifier = modifier,
        )
    }
}

@Composable
fun SettingsScreen(
    settings: Settings,
    onChangeGoal: (Int) -> Unit,
    onChooseTheme: (ThemeChoice) -> Unit,
    modifier: Modifier = Modifier,
    onRemindersChange: (Boolean) -> Unit = {},
    remindersDenied: Boolean = false,
    onTestReminder: (() -> Unit)? = null,
) {
    Surface(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Daily goal", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onChangeGoal(-5) }, enabled = settings.dailyGoal > Settings.GOAL_RANGE.first) { Text("Fewer") }
                Text("${settings.dailyGoal} cards a day")
                OutlinedButton(onClick = { onChangeGoal(5) }, enabled = settings.dailyGoal < Settings.GOAL_RANGE.last) { Text("More") }
            }
            // The whole row toggles, so the label is a big touch target and screen readers announce one switch.
            Row(
                Modifier.fillMaxWidth().toggleable(value = settings.reminders, role = Role.Switch, onValueChange = onRemindersChange),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Daily reminder", style = MaterialTheme.typography.titleMedium)
                    Text("A notification when cards are due", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = settings.reminders, onCheckedChange = null)
            }
            onTestReminder?.let { OutlinedButton(onClick = it) { Text("Send a test reminder") } }
            if (remindersDenied) {
                Text(
                    "NoteDeck can't post notifications. You can allow them in the system settings.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
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
    NoteDeckTheme { SettingsScreen(Settings(dailyGoal = 25, theme = ThemeChoice.DARK, reminders = true), onChangeGoal = {}, onChooseTheme = {}) }
}

@Preview(name = "Reminders refused", showBackground = true)
@Composable
private fun SettingsDeniedPreview() {
    NoteDeckTheme { SettingsScreen(Settings(), onChangeGoal = {}, onChooseTheme = {}, remindersDenied = true) }
}
