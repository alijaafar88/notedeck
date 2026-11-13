# Q2: Modules, a Baseline Profile and a signed bundle

Everything Android lives in `:app`. Split it the way Now in Android does, share the build setup, make cold start faster, and produce a release bundle you could upload. The Gradle plugins for build logic, the Baseline Profile plugin, Macrobenchmark, UI Automator and profileinstaller are in the version catalog.

- [ ] Split NoteDeck into `:core:model`, `:core:domain`, `:core:data`, `:core:ui`, `:feature:decks` and `:feature:study`, with `:app` wiring them together
- [ ] A `build-logic` convention plugin replaces the Android setup each module repeated
- [ ] A `:baselineprofile` module generates a profile from a real journey, and the app ships it with `profileinstaller`
- [ ] The release build runs R8 and still loads shared decks
- [ ] Signing reads from environment variables or a gitignored `keystore.properties`, and `bundleRelease` produces a signed `.aab`

When you're done:
- `./gradlew test assembleRelease` passes (unsigned without a key).
- With your own upload key, `bundleRelease` writes a signed bundle; check it with `jarsigner -verify`. Never commit the keystore.
- On an emulator running API 33 or later, `./gradlew :app:generateBaselineProfile` writes the profile.

Then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `q2-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
