# M1: ViewModels with UiState

Move the Deck List and Study screens' state into ViewModels, backed by the fake repository from K4.

- [ ] `DecksViewModel` exposes a `StateFlow<DecksUiState>` built with `stateIn` and `WhileSubscribed(5_000)`
- [ ] `DecksUiState` is a sealed interface: `Loading`, `Content` and `Empty`
- [ ] `StudyViewModel` holds the session and keeps its position in `SavedStateHandle`
- [ ] Each screen is a route that collects with `collectAsStateWithLifecycle`, and a stateless screen with previews
- [ ] ViewModel tests with Turbine, against `FakeDeckRepository`

When you're done, run the app on an emulator and run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `m1-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
