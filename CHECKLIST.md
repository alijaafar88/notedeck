# AI1: Agent-written tests, checked with mutations

NoteDeck has tests, but are they any good? This step brings in a coding agent (Gemini in Android Studio's Agent Mode, Claude Code, or another) to write more of them, and then checks the tests by breaking the code on purpose.

- [ ] Add `AGENTS.md` at the root: how to build and test, the architecture rules an agent can't guess from the code, and what it must never touch. Add a one-line `CLAUDE.md` that points to it.
- [ ] Add `.aiexclude` so Android Studio's AI features never read `keystore.properties`, `local.properties` or key files
- [ ] Ask an agent for a plan first, then tests for `reviewIntervalDays`, `isDue` and `RoomDeckRepository`. Review its diff with the AI1.4 checklist before you keep any of it
- [ ] Make at least three deliberate bugs (mutations), one at a time, for example:
  - `isDue`: `>=` becomes `>`
  - `reviewIntervalDays`: drop the `coerceIn`
  - `recordReview`: write nothing
- [ ] For each mutation, run the tests, and record in `MUTATION.md` whether a test failed (caught) or not (survived), and which test. A mutant that survives means a missing test: add one and run the mutant again
- [ ] Revert every mutation

When you're done:
- `./gradlew test` passes on the unmutated code.
- Every mutation in `MUTATION.md` is caught by a named test.
- No existing test was weakened or deleted to make the build green.

Then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `ai1-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
