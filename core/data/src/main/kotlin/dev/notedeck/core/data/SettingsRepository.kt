package dev.notedeck.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/**
 * reminders is off until the learner turns it on, which is also when Android 13+ asks for the notification permission.
 * allowCloudAi is off too: nothing goes to a cloud model as a fallback until the learner says it may.
 */
data class Settings(
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val reminders: Boolean = false,
    val allowCloudAi: Boolean = false,
    /** The study coach's applied plan, if any: the decks to focus on, and why. */
    val focusDecks: List<String> = emptyList(),
    val planReason: String = "",
) {
    companion object {
        const val DEFAULT_DAILY_GOAL = 20
        val GOAL_RANGE = 5..200
    }
}

/** A few small values, not tables: Preferences DataStore. The DataStore itself is a singleton from Hilt. */
@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    val settings: Flow<Settings> = store.data.map { preferences ->
        Settings(
            dailyGoal = preferences[DAILY_GOAL] ?: Settings.DEFAULT_DAILY_GOAL,
            theme = ThemeChoice.entries.firstOrNull { it.name == preferences[THEME] } ?: ThemeChoice.SYSTEM,
            reminders = preferences[REMINDERS] ?: false,
            allowCloudAi = preferences[ALLOW_CLOUD_AI] ?: false,
            focusDecks = preferences[FOCUS_DECKS]?.split("\n")?.filter(String::isNotEmpty).orEmpty(),
            planReason = preferences[PLAN_REASON].orEmpty(),
        )
    }

    suspend fun setDailyGoal(goal: Int) {
        store.edit { it[DAILY_GOAL] = goal.coerceIn(Settings.GOAL_RANGE) }
    }

    suspend fun setTheme(theme: ThemeChoice) {
        // Stored by name, so reordering ThemeChoice can't change a saved choice.
        store.edit { it[THEME] = theme.name }
    }

    suspend fun setReminders(on: Boolean) {
        store.edit { it[REMINDERS] = on }
    }

    suspend fun setAllowCloudAi(allowed: Boolean) {
        store.edit { it[ALLOW_CLOUD_AI] = allowed }
    }

    suspend fun currentSettings(): Settings = settings.first()

    /** One edit, so the goal and the plan it came from never disagree. */
    suspend fun applyPlan(dailyGoal: Int, focusDecks: List<String>, reason: String) {
        store.edit {
            it[DAILY_GOAL] = dailyGoal.coerceIn(Settings.GOAL_RANGE)
            it[FOCUS_DECKS] = focusDecks.joinToString("\n")
            it[PLAN_REASON] = reason
        }
    }

    private companion object {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val THEME = stringPreferencesKey("theme")
        val REMINDERS = booleanPreferencesKey("reminders")
        val ALLOW_CLOUD_AI = booleanPreferencesKey("allow_cloud_ai")
        val FOCUS_DECKS = stringPreferencesKey("focus_decks")
        val PLAN_REASON = stringPreferencesKey("plan_reason")
    }
}
