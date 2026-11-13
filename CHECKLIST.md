# AI2: A Prompt Playground with a fake model

Before NoteDeck talks to a real model, give it a place to try prompts, and an interface that every model can sit behind. In this step the model is a fake; AI3 and AI4 plug Gemini, Claude and Gemini Nano into the same interface.

- [ ] In `:core:domain`, add a `TextModel` interface that streams a reply as a `Flow<String>` for a request holding the prompt and the temperature. No SDK types in it.
- [ ] Add `FakeTextModel`: the same reply every time at temperature 0, varied above it, streamed a word at a time with a short delay
- [ ] Add a `:feature:playground` module (the feature convention plugin) with a `PlaygroundViewModel`: an immutable `PlaygroundUiState` (prompt, temperature, output, running, error) exposed as a `StateFlow`
- [ ] Running again cancels the previous run, and Stop cancels the current one
- [ ] Add a stateless `PlaygroundScreen` (prompt field, temperature slider, Run or Stop, the streamed output) with previews, and a `PlaygroundRoute`
- [ ] Bind `TextModel` to the fake in `:app`, and show a "Prompt Playground" button in Settings in debug builds only
- [ ] Test the ViewModel with fakes: the output streams in, a new run replaces the old one, the temperature reaches the model, an error is shown

When you're done:
- `./gradlew test assembleDebug assembleRelease` passes.
- In a debug build, Settings opens the Playground; at temperature 0, running the same prompt twice gives the same output.
- A release build has no way to reach the Playground.

Then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `ai2-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
