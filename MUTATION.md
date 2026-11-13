# Mutation results

Each mutation is one deliberate bug, applied alone, then `./gradlew :core:domain:test :core:data:testDebugUnitTest`, then reverted. "Caught" means at least one test failed.

## Before (the q2-solution tests)

| # | Mutation | Result |
| --- | --- | --- |
| M1 | `isDue`: `>=` becomes `>` | caught |
| M2 | `reviewIntervalDays`: no `coerceIn`, so box 0 or 6 gets the wrong interval | **survived** |
| M3 | `nextReview`: EASY moves up one box, not two | caught |
| M4 | `isDue`: a card never reviewed is not due (`== null \|\|` becomes `!= null &&`) | caught |
| M5 | `addCard`: new cards go to position 0, not the end | caught |
| M6 | `recordReview`: writes nothing | **survived** |

Two survivors: nothing tested boxes outside 1 to 5, and nothing read a review back.

## After (ai1-solution)

| # | Result | Caught by |
| --- | --- | --- |
| M1 | caught | `LeitnerTest`: a card is due when its interval has passed; in every box, a card falls due exactly one interval after its review |
| M2 | caught | `LeitnerTest`: boxes outside 1 to 5 use the nearest box's interval |
| M3 | caught | `LeitnerTest`: moves cards between boxes |
| M4 | caught | `LeitnerTest`: a card is due when its interval has passed, or it was never reviewed |
| M5 | caught | `RoomDeckRepositoryTest`: an added card is read back at the end of its deck |
| M6 | caught | `RoomDeckRepositoryTest`: a recorded review is stored, with its grade and time |

## A test we dropped

"Cards added one by one keep their order" looked useful, but it passed on M5 too: into an empty deck, every card at position 0 still comes back in insertion order. A test that can't fail on the bug it's named for adds reading time and no safety, so it's gone. The existing "at the end of its deck" test catches M5, because the seeded cards already hold positions 0 and up.

Every mutation was reverted; `./gradlew test` passes on the real code.
