package dev.notedeck.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest

/**
 * A DataStore kept in memory: the repository's logic (defaults, range, stored names) is what's under test here.
 * The real file-backed DataStore needs a device; on a Windows JVM its second write fails to rename over the first.
 */
class InMemoryPreferencesStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    private val lock = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        lock.withLock { transform(state.value).also { state.value = it } }
}

class SettingsRepositoryTest {
    private val repository = SettingsRepository(InMemoryPreferencesStore())

    @Test
    fun `nothing saved reads as the defaults`() = runTest {
        assertEquals(Settings(dailyGoal = 20, theme = ThemeChoice.SYSTEM), repository.settings.first())
    }

    @Test
    fun `saved values are read back`() = runTest {
        repository.setDailyGoal(35)
        repository.setTheme(ThemeChoice.DARK)
        assertEquals(Settings(dailyGoal = 35, theme = ThemeChoice.DARK), repository.settings.first())
    }

    @Test
    fun `the daily goal stays in range`() = runTest {
        repository.setDailyGoal(1_000)
        assertEquals(Settings.GOAL_RANGE.last, repository.settings.first().dailyGoal)
        repository.setDailyGoal(0)
        assertEquals(Settings.GOAL_RANGE.first, repository.settings.first().dailyGoal)
    }
}
