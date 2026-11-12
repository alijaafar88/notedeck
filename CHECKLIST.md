# C3: An Add Card screen with hoisted state

Build a form whose state lives in one place, with validation and a snackbar.

- [ ] `AddCardScreen` is stateless: it takes front, back and callbacks
- [ ] A route composable owns the form state with `rememberSaveable`
- [ ] Save is disabled until both fields have text
- [ ] Saving shows a snackbar from a coroutine, not from composition

When you're done, run the app on an emulator and run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `c3-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
