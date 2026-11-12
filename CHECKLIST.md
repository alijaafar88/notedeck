# D1: Persist NoteDeck with Room and DataStore

NoteDeck forgets everything when the app closes. Put Room behind the repository and DataStore behind the settings. Room 3, the SQLite driver and Preferences DataStore are in the version catalog, and the Room plugin is declared in the root build, ready to apply.

- [ ] Add Room 3 with KSP and `AndroidSQLiteDriver`, and export schemas to `app/schemas`
- [ ] `DeckEntity`, `CardEntity` (a foreign key, indexed) and `ReviewEntity`, with an `Instant` converter
- [ ] A `DeckDao` with `Flow` reads, `suspend` writes and a `@Transaction` query for a deck with its cards
- [ ] `RoomDeckRepository` maps entities to domain models and replaces `SampleDeckRepository` in the Hilt module; sample decks are seeded on first launch
- [ ] The daily goal and theme live in Preferences DataStore, with a Settings screen
- [ ] An in-memory DAO test and a settings test pass

When you're done, run the app on an emulator, add a card, force-stop the app and check the card is still there. Run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `d1-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
