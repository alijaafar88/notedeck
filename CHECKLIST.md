# AI3: Generate cards from notes, with Gemini or Claude

Learners paste notes, or photograph them, and NoteDeck drafts flashcards. The learner watches the draft stream in, edits or deletes each card, and saves the ones they want. Two cloud models sit behind one interface, and neither puts an API key in the app. Firebase, Ktor and the Anthropic Java SDK are in the version catalog.

- [ ] In `:core:domain`, add a `CardGenerator` interface that streams events (text pieces, then the parsed drafts), and the rules every draft must pass: not blank, short enough, not already in the deck
- [ ] In `:core:data`, implement it three ways:
  - Gemini through Firebase AI Logic, with a JSON response schema and photos as input
  - Claude through your own backend, reading its server-sent events with OkHttp
  - a fake, for tests and for running without either
- [ ] Initialise Firebase from `local.properties` values, never a committed file. Without them, Gemini shows as unavailable and the app still runs. Install App Check (Play Integrity) for release builds
- [ ] Add `backend/`: a Ktor server that reads `ANTHROPIC_API_KEY` from its environment, caps the size of the notes, rate-limits each client, and streams Claude's reply as server-sent events
- [ ] Add a Generate screen, opened from a deck: notes or a photo, a provider choice, the streamed draft, then an editable list of cards and Save
- [ ] Test the rules, the ViewModel with a fake generator, the Claude client against MockWebServer, and the backend with a fake Claude

When you're done:
- `./gradlew test assembleDebug assembleRelease` passes with no Firebase values and no API key.
- In a debug build on the emulator, generating with the fake drafts cards, and saving adds them to the deck.
- With `ANTHROPIC_API_KEY` set, `./gradlew :backend:run` serves `POST /cards`, and the Claude option works from the emulator.
- `git grep` finds no API key anywhere.

Then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `ai3-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
