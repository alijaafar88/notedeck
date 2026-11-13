package dev.notedeck.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.notedeck.app.data.SettingsRepository
import dev.notedeck.app.data.SharedDeckRepository
import dev.notedeck.app.network.NetworkResult
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlinx.coroutines.flow.first

/** Once a day: if reminders are on and anything is due, say how many. Thin: the counting lives in the data layer. */
@HiltWorker
class ReviewReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val dueCards: DueCardCounter,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!settings.settings.first().reminders || !notifier.canNotify()) return Result.success()
        val due = dueCards.countDue(clock.now())
        if (due > 0) notifier.showReviewsDue(due)
        return Result.success()
    }
}

/** Nightly: refresh the shared catalog. Offline or a server error means try again later, up to three attempts. */
@HiltWorker
class SharedDeckSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val shared: SharedDeckRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (shared.refresh()) {
        is NetworkResult.Success -> Result.success()
        is NetworkResult.HttpError, NetworkResult.Offline -> if (runAttemptCount < MAX_ATTEMPTS - 1) Result.retry() else Result.failure()
    }

    companion object {
        const val MAX_ATTEMPTS = 3
    }
}

const val REMINDER_WORK = "daily-review-reminder"
const val SYNC_WORK = "nightly-shared-deck-sync"

/** Safe at every launch: unique work with KEEP never schedules a second copy. */
fun scheduleBackgroundWork(workManager: WorkManager) {
    val reminder = PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS).build()
    workManager.enqueueUniquePeriodicWork(REMINDER_WORK, ExistingPeriodicWorkPolicy.KEEP, reminder)

    val sync = PeriodicWorkRequestBuilder<SharedDeckSyncWorker>(1, TimeUnit.DAYS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    workManager.enqueueUniquePeriodicWork(SYNC_WORK, ExistingPeriodicWorkPolicy.KEEP, sync)
}
