# NoteDeck

Starting code and solutions for the build steps in KotlinDroid Daily. Over the course you build NoteDeck, a notes-to-flashcards study app.

Each step has two tags:

| Tag | What it is |
| --- | --- |
| `k1-start` ... `k4-start` | Where the step begins |
| `k1-solution` ... `k4-solution` | One finished version to compare with |

To work on a step:

```bash
git clone https://github.com/alijaafar88/notedeck.git
cd notedeck
git switch -c my-k4 k4-start
```

Then follow `CHECKLIST.md`. Every start tag builds, and every solution tag passes `./gradlew test`. Later units add more tags as the course grows.

## Why Deck List is MVVM and Study is MVI

Deck List shows whatever the repository holds and has no rules of its own, so a ViewModel that maps a Flow to `DecksUiState` with `stateIn` is all it needs; intent classes and a reducer would add code without catching any bugs. Study is different: position, the known count, whether the answer is showing and whether the session is over must always agree, and several actions change them. Funnelling every action through one pure `reduce(state, intent)` keeps those values consistent, lets a table test cover every intent in a few lines, and makes a bug reproducible from the list of intents that caused it. One-off navigation goes through a buffered `Channel`, so it's neither lost nor repeated across rotation.

## Running the AI features

NoteDeck never holds a provider API key. Everything below is optional: without it, a fresh clone still builds, and the missing providers show as "not set up".

**Gemini (Firebase AI Logic).** Create a Firebase project, add an Android app with the package `dev.notedeck`, and enable Firebase AI Logic with the Gemini Developer API. Then add the app's values to `local.properties` (gitignored). They identify the project but aren't secrets; App Check is what protects it.

```properties
notedeck.firebase.appId=1:1234567890:android:abc123
notedeck.firebase.apiKey=AIza...
notedeck.firebase.projectId=your-project
```

Release builds install App Check with Play Integrity; enforce it in the Firebase console before you ship.

**Claude (your own backend).** `backend/` is a small Ktor server that holds the Anthropic key and streams Claude's reply to the app. Put the key in the server's environment, never in a file in Git:

```bash
ANTHROPIC_API_KEY=... ./gradlew :backend:run
```

Debug builds on the emulator reach it at `http://10.0.2.2:8080`. To use another address, set `notedeck.proxyUrl` in `local.properties`; anything other than the emulator needs https.

## Evals

`evals/` checks the card generator against 30 golden notes, including notes with no facts and three prompt-injection attempts. Every push runs it in CI against stored replies, and fails below a 90% pass rate or on any injection failure:

```bash
./gradlew :evals:run
```

The stored replies in `evals/data/reference-replies.json` are hand-written references, not model output, so they test the eval runner and the rules, not the model. To test the model, run it live with a key in your environment, and add `--record` to save its replies as the new reference set:

```bash
ANTHROPIC_API_KEY=... ./gradlew :evals:run --args="live --record"
```

CI runs the live step too, when the repository has an `ANTHROPIC_API_KEY` secret.
