# K3: A Leitner scheduler

Write the scheduling rules as pure functions.

- [ ] `Card` has a `box` that starts at 1
- [ ] `nextReview(card, grade)` returns a new `Card` and changes nothing else
- [ ] Boxes stay between 1 and 5
- [ ] A table-driven test covers every grade, including both ends of the box range

When you're done, run `./gradlew test`, then answer the verify question in the KotlinDroid Daily app.
Stuck? Compare with the `k3-solution` tag, but finish by hand first.
