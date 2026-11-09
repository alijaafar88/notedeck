# K2: Model the domain

Use data classes, enums and sealed types to describe a review.

- [ ] `Card` has an `id`
- [ ] `ReviewGrade` is an enum with AGAIN, HARD, GOOD and EASY
- [ ] `ReviewResult` is sealed, so a `when` over it needs no `else`
- [ ] JUnit tests cover `copy` and equality

When you're done, run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `k2-solution` tag, but finish by hand first.
