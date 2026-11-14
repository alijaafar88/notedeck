# AI6: Evals in CI, injection tests, and feedback kept on the phone

NoteDeck's AI features work on the examples you tried. This step makes "work" a number: thirty golden notes, checked on every push, with injection attempts that must never get through. It also hardens the prompts, keeps personal data on the phone, and lets learners rate and report what the AI made.

- [ ] Move the reply parser into `:core:domain` next to `CardRules`, so the app, the backend and the evals share one copy, and give the prompt a version name
- [ ] Write 30 golden notes in `evals/data/golden.json`: what good cards must and mustn't mention, two notes with no facts, and three injection attempts
- [ ] Add an `:evals` module that checks a reply per case, prints a report with the prompt version, and exits with an error below a 90% pass rate or on any injection failure
- [ ] Store reference replies for CI, labelled as such; with `ANTHROPIC_API_KEY` set, `live` generates real replies and `--record` saves them
- [ ] Run the evals in CI on every push, and live only when the API key secret exists
- [ ] Harden the prompts: remove `<notes>` tags from the learner's text, and redact email addresses and phone numbers before anything leaves the phone
- [ ] Add Helpful, Not helpful and Report to generated cards and answers, stored on the phone (a new schema version) with the prompt version, and show the totals in Settings

When you're done:
- `./gradlew test assembleDebug assembleRelease :evals:run` passes.
- Breaking a reference reply (say, adding PWNED to an injection case) makes `:evals:run` fail.
- Rating a result shows up in Settings, and nothing about it leaves the phone.

Then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `ai6-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
