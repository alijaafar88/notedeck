package dev.notedeck.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import dev.notedeck.core.data.work.AndroidReminderNotifier
import dev.notedeck.core.data.work.scheduleBackgroundWork
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
        initFirebase()
    }

    /**
     * Firebase from local.properties values, not a committed google-services.json, so a fresh clone builds.
     * Without them Gemini is unavailable and everything else works. Release builds install App Check with
     * Play Integrity; enforce it in the Firebase console before shipping. For debug builds, add the
     * firebase-appcheck-debug provider and register its token, or leave enforcement off while developing.
     */
    private fun initFirebase() {
        val values = listOf(BuildConfig.FIREBASE_APP_ID, BuildConfig.FIREBASE_API_KEY, BuildConfig.FIREBASE_PROJECT_ID)
        if (values.any { it.isBlank() } || FirebaseApp.getApps(this).isNotEmpty()) return
        val options = FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .build()
        FirebaseApp.initializeApp(this, options)
        if (!BuildConfig.DEBUG) Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
