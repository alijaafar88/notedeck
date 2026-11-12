# M2: The Study screen as MVI

Rebuild the Study screen as MVI. Deck List stays MVVM.

- [ ] `StudyState` is one immutable data class for the whole session
- [ ] `StudyIntent` is a sealed interface: `DeckLoaded`, `Reveal`, `Answer(knewIt)`, `Restart` and `Done`
- [ ] A pure `reduce(state, intent)` handles every intent, with a table test
- [ ] `Done` sends a `Close` effect through a buffered `Channel`; the route collects it in a `LaunchedEffect` and navigates back
- [ ] Deck List stays MVVM; the README explains the choice in one paragraph

When you're done, run the app on an emulator and run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `m2-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
