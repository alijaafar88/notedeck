package dev.notedeck.core.data.work

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import dev.notedeck.core.data.FakeSharedDecksApi
import dev.notedeck.core.data.InMemoryPreferencesStore
import dev.notedeck.core.data.MutableClock
import dev.notedeck.core.data.NoteDeckDatabase
import dev.notedeck.core.data.RoomDeckRepository
import dev.notedeck.core.data.SettingsRepository
import dev.notedeck.core.data.SharedDeckRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private class FakeNotifier(private val allowed: Boolean = true) : ReminderNotifier {
    val shown = mutableListOf<Int>()

    override fun canNotify() = allowed

    override fun showReviewsDue(count: Int) {
        shown += count
    }
}

private class FixedDueCards(private val due: Int) : DueCardCounter {
    override suspend fun countDue(now: Instant) = due
}

/** Workers are built directly, with a factory that hands them fakes, the way HiltWorkerFactory does in the app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class WorkersTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = MutableClock(Instant.parse("2026-11-12T18:00:00Z"))
    private val db = Room.inMemoryDatabaseBuilder<NoteDeckDatabase>(context).setDriver(AndroidSQLiteDriver()).build()

    @AfterTest
    fun tearDown() = db.close()

    private fun factory(create: (Context, WorkerParameters) -> ListenableWorker) = object : WorkerFactory() {
        override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) = create(appContext, workerParameters)
    }

    private suspend fun reminder(due: Int, remindersOn: Boolean, notifier: FakeNotifier): ListenableWorker.Result {
        val settings = SettingsRepository(InMemoryPreferencesStore()).apply { setReminders(remindersOn) }
        val worker = TestListenableWorkerBuilder<ReviewReminderWorker>(context)
            .setWorkerFactory(factory { c, p -> ReviewReminderWorker(c, p, settings, FixedDueCards(due), notifier, clock) })
            .build()
        return worker.doWork()
    }

    @Test
    fun `cards due and reminders on, one notification with the count`() = runTest {
        val notifier = FakeNotifier()
        assertEquals(ListenableWorker.Result.success(), reminder(due = 12, remindersOn = true, notifier = notifier))
        assertEquals(listOf(12), notifier.shown)
    }

    @Test
    fun `nothing due, no notification`() = runTest {
        val notifier = FakeNotifier()
        reminder(due = 0, remindersOn = true, notifier = notifier)
        assertEquals(emptyList(), notifier.shown)
    }

    @Test
    fun `reminders off or no permission, no notification`() = runTest {
        val off = FakeNotifier()
        reminder(due = 5, remindersOn = false, notifier = off)
        val refused = FakeNotifier(allowed = false)
        reminder(due = 5, remindersOn = true, notifier = refused)
        assertEquals(emptyList(), off.shown + refused.shown)
    }

    private fun sync(api: FakeSharedDecksApi, attempt: Int = 0): SharedDeckSyncWorker {
        val shared = SharedDeckRepository(api, db.sharedDeckDao(), RoomDeckRepository(db.deckDao()), clock)
        return TestListenableWorkerBuilder<SharedDeckSyncWorker>(context)
            .setWorkerFactory(factory { c, p -> SharedDeckSyncWorker(c, p, shared) })
            .setRunAttemptCount(attempt)
            .build()
    }

    @Test
    fun `a sync writes the catalog and succeeds`() = runTest {
        assertEquals(ListenableWorker.Result.success(), sync(FakeSharedDecksApi()).doWork())
        assertEquals(1, db.sharedDeckDao().observeAll().first().size)
    }

    @Test
    fun `offline retries, then gives up on the third attempt`() = runTest {
        val offline = FakeSharedDecksApi().apply { this.offline = true }
        assertEquals(ListenableWorker.Result.retry(), sync(offline).doWork())
        assertEquals(ListenableWorker.Result.failure(), sync(offline, attempt = 2).doWork())
    }
}
