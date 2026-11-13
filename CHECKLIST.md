# D2: Import shared decks, offline-first

Shared decks are JSON files in this repo's `decks/` folder, served by GitHub Pages at `https://alijaafar88.github.io/notedeck/decks/`. Retrofit, OkHttp, MockWebServer and kotlinx-serialization-json are in the version catalog, ready to add.

- [ ] Read the shared decks served from the companion repo's GitHub Pages site: `decks/index.json` and one file per deck
- [ ] A Retrofit `SharedDecksApi` with the kotlinx.serialization converter, built once in Hilt, and the `INTERNET` permission
- [ ] The catalog is cached in a `shared_decks` table: schema version 2, with an `AutoMigration` from version 1
- [ ] A Shared decks screen reads only from Room, refreshes when the cache is stale, and imports a deck into your own decks
- [ ] A sealed `NetworkResult`, and an offline banner driven by a `callbackFlow` over `ConnectivityManager`
- [ ] MockWebServer tests for the API, and a migration test from version 1

When you're done, run the app on an emulator, open Shared decks, import one, then turn on airplane mode and reopen the screen: the catalog should still be there, under an offline banner. Run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `d2-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
