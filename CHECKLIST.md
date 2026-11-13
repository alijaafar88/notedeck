# AI4: Summarise notes on the phone, and scan notes from a photo

Two on-device features for the Generate screen: a summary of the notes from Gemini Nano, falling back to the cloud only when the learner allows it, and text read from a photo of notes, entirely on the phone. ML Kit GenAI summarization (beta), ML Kit text recognition and the coroutine adapters are in the version catalog.

- [ ] In `:core:domain`, add a `Summarizer` interface (status, download, summarize) and a router that picks the phone first, then the cloud only if the learner allowed it, and otherwise says why it can't
- [ ] In `:core:data`, implement it with ML Kit GenAI summarization (check the feature, download it, run, close) and with your backend (add `POST /summary` there)
- [ ] Add a "Use cloud AI when this phone can't" setting, off by default
- [ ] Add an `OcrScanner` interface and an ML Kit text recognition implementation; "Scan notes from a photo" adds the photo's text to the notes
- [ ] Show every summary state on the Generate screen: on this phone, in the cloud, needs a download, downloading, not on this phone, running, failed
- [ ] Test the router as a table, the ViewModel with fakes for each state, and the backend's new route

When you're done:
- `./gradlew test assembleDebug assembleRelease` passes.
- On the emulator (which has no Gemini Nano): with cloud AI off, the screen explains why there's no summary; with it on, the summary comes from the backend.
- Scanning a photo of printed notes fills in their text, with the network off.

Then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `ai4-solution` tag, but finish by hand first. Afterwards, try the AI-pair follow-up from the app.
