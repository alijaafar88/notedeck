# K4: A Flow-backed DeckRepository

Expose decks as a stream that the console app and tests can both collect.

- [ ] `DeckRepository` exposes `Flow<List<Deck>>`
- [ ] `FakeDeckRepository` uses `MutableStateFlow` internally and exposes it read-only
- [ ] The console app collects the flow and prints each update
- [ ] A Turbine test asserts a new emission after `addDeck()`

When you're done, run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `k4-solution` tag, but finish by hand first.
