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
