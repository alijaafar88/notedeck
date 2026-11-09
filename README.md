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
