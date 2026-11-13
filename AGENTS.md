# NoteDeck: notes for coding agents

NoteDeck is a flashcard app built step by step in the KotlinDroid Daily course. Kotlin, Jetpack Compose, Room 3, Hilt, WorkManager.

## Build and check
- Build: `./gradlew assembleDebug`. Test: `./gradlew test`. Both must pass before you say you're done.
- `./gradlew assembleRelease` runs R8; run it if you touch serialization, Retrofit or reflection.
- Never delete, skip or weaken a test to make the build pass. If a test looks wrong, say so and stop.

## Architecture
- Modules: `:app`, `:core:{model,domain,data,ui,testing}`, `:feature:{decks,study}`, `:baselineprofile`, `:cli`.
- `:feature:*` depend on `:core:*` only, never on each other or on `:core:data`. `:app` wires them together.
- Shared build setup lives in `build-logic/` convention plugins; don't repeat it in module build files.
- ViewModels expose `StateFlow`, never `MutableStateFlow` or LiveData. Screens collect with `collectAsStateWithLifecycle`.
- Each screen is a stateful `*Route` plus a stateless `*Screen` with previews.
- Study is MVI: every action goes through the pure `reduce(state, intent)`; add a row to its table test.
- Repositories are the only way to reach Room or the network. No `Context` in ViewModels, no `GlobalScope`; inject dispatchers.
- KSP, never kapt. Versions live in `gradle/libs.versions.toml`. Ask before adding a dependency.

## Tests
- Plain JVM tests for `:core:model` and `:core:domain`; Robolectric for Room, Compose and workers.
- Check exact values. A test that only checks "not null" or "greater than 0" won't catch a real bug; see `MUTATION.md`.

## Never
- Never read, print or edit `keystore.properties`, `local.properties`, `*.jks` or any API key.
- Never commit generated files under `build/`, heap dumps or keys.
