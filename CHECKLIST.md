# D3: A daily reminder and a nightly sync

NoteDeck should nudge the learner when cards are due and keep the shared catalog fresh, even when the app isn't open. WorkManager, hilt-work, its KSP compiler and work-testing are in the version catalog, ready to add.

- [ ] WorkManager with Hilt: `hilt-work`, `Configuration.Provider` on `NoteDeckApplication`, and the default initializer removed from the manifest
- [ ] `ReviewReminderWorker` posts "N reviews due" once a day on its own channel, only when something is due
- [ ] `SharedDeckSyncWorker` refreshes the shared catalog nightly: unique periodic work, needs a network, exponential backoff
- [ ] Settings has a Reminders switch that asks for `POST_NOTIFICATIONS` on Android 13+ and respects a no
- [ ] Worker tests with `TestListenableWorkerBuilder` and a `WorkerFactory` that passes fakes

When you're done, turn reminders on in Settings and use a debug-only button that enqueues a one-time `ReviewReminderWorker`, so you can see the notification without waiting a day. Forcing the periodic job with `adb shell cmd jobscheduler run` won't do it: WorkManager skips periodic work that runs before its schedule. Run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `d3-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
