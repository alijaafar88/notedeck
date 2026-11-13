package dev.notedeck.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import dev.notedeck.app.work.AndroidReminderNotifier
import dev.notedeck.app.work.scheduleBackgroundWork
import javax.inject.Inject

/**
 * Creates Hilt's app-wide component, and gives WorkManager Hilt's factory so workers get their dependencies.
 * WorkManager's default initializer is removed in the manifest, so it starts with this configuration.
 */
@HiltAndroidApp
class NoteDeckApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var notifier: AndroidReminderNotifier

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()
        scheduleBackgroundWork(WorkManager.getInstance(this))
    }
}
