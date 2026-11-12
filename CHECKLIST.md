# M3: From an AppContainer to Hilt

Wire NoteDeck by hand first, then let Hilt do it. The Hilt and KSP plugins and libraries are in the version catalog, ready to apply.

- [ ] First by hand: replace `ServiceLocator` with an `AppContainer` owned by `NoteDeckApplication`
- [ ] Then Hilt: `@HiltAndroidApp` on the Application and `@AndroidEntryPoint` on `MainActivity`
- [ ] `DecksViewModel` and `StudyViewModel` are `@HiltViewModel` with `@Inject` constructors, obtained with `hiltViewModel()`
- [ ] A module `@Binds` `DeckRepository` as a `@Singleton`, and an `@IoDispatcher` qualifier provides `Dispatchers.IO`
- [ ] A Hilt test swaps in a fake repository with `@TestInstallIn`

When you're done, run the app on an emulator and run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `m3-solution` tag, but finish by hand first. Its code shows only the final Hilt version, because Hilt replaces the AppContainer. Afterwards, try the AI-pair follow-up from the app.
