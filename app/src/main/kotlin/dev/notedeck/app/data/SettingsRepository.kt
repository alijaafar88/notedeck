package dev.notedeck.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

data class Settings(val dailyGoal: Int = DEFAULT_DAILY_GOAL, val theme: ThemeChoice = ThemeChoice.SYSTEM) {
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
        )
    }

    suspend fun setDailyGoal(goal: Int) {
        store.edit { it[DAILY_GOAL] = goal.coerceIn(Settings.GOAL_RANGE) }
    }

    suspend fun setTheme(theme: ThemeChoice) {
        // Stored by name, so reordering ThemeChoice can't change a saved choice.
        store.edit { it[THEME] = theme.name }
    }

    private companion object {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val THEME = stringPreferencesKey("theme")
    }
}
