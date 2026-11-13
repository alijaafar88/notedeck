# Q1: A test suite and CI

NoteDeck has gathered tests unit by unit. Make them a deliberate suite, add UI tests for two real flows, and run everything on every push. The Compose UI test libraries are in the version catalog, ready to add.

- [ ] A `MainDispatcherRule`, used by every ViewModel test instead of `setMain` and `resetMain` by hand
- [ ] `DecksViewModel`'s exact state sequence with Turbine: Loading, Empty, then Content
- [ ] Two Compose UI flows under Robolectric, with the v2 `createComposeRule`: adding a card updates the deck, and studying a deck to the end
- [ ] A GitHub Actions workflow runs `./gradlew test assembleDebug` on every push to main and every pull request
- [ ] Reducers, ViewModels, repositories and the two flows are all covered

When you're done, push and watch the Actions tab on GitHub: the run should go green. Run `./gradlew test` locally too, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `q1-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
