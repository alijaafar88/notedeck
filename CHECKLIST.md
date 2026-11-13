# AI5: "Ask my notes" with citations, and a study coach you approve

Two features built from this unit. Ask answers questions from the learner's own notes, citing them, and says so when they don't hold the answer. The study coach is an agent on the backend that looks at the learner's statistics through read-only tools and proposes a week's plan, which changes nothing until the learner applies it. Koog is in the version catalog.

- [ ] Keep the notes behind generated cards: chunk them, embed each chunk, and store the vectors in Room (schema version 3, with a migration test)
- [ ] Add an `Embedder` interface; a small hashing embedder runs on the phone for now
- [ ] Add a note index: cosine similarity over the stored vectors, top k, and a score threshold below which Ask answers "Not in your notes." without calling a model
- [ ] Add `POST /answer` to the backend: numbered sources in, an answer citing them like [1] streamed out
- [ ] Add an Ask screen: the answer streams in, each citation shows its passage, and an answer with no valid citations is flagged
- [ ] Add `POST /coach`: a Koog agent with read-only tools over a snapshot of the learner's statistics, a step limit, and a JSON plan out
- [ ] Add a Coach screen: the plan is validated (known decks, a sensible goal), shown, and applied only when the learner taps Apply
- [ ] Test the embedder, chunker, search, grounding checks, plan validation, both ViewModels with fakes, and the backend's routes and tools

When you're done:
- `./gradlew test assembleDebug assembleRelease` passes.
- Asking about something not in your notes answers "Not in your notes." with no network.
- A proposed plan changes your daily goal only after you tap Apply.

Then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `ai5-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
