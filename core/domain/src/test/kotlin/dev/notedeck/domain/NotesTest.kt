package dev.notedeck.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotesTest {
    private val embedder = HashingEmbedder()

    private fun score(a: String, b: String) = cosine(embedder.embed(a), embedder.embed(b))

    @Test
    fun `shared words score higher than unrelated text, plurals included`() {
        val question = "What is a channel?"
        assertTrue(score(question, "Channels: hot pipes between coroutines") > score(question, "Room stores data in SQLite"))
        assertTrue(score(question, "Channels: hot pipes between coroutines") >= Grounding.MIN_SCORE)
        assertTrue(score(question, "Room stores data in SQLite") < Grounding.MIN_SCORE)
    }

    @Test
    fun `vectors are normalised, and empty text is the zero vector`() {
        val v = embedder.embed("cold flows and hot channels")
        assertEquals(1f, kotlin.math.sqrt(v.sumOf { (it * it).toDouble() }).toFloat(), 1e-5f)
        assertEquals(0f, cosine(embedder.embed("the of and"), v))
    }

    @Test
    fun `chunks follow blank lines, stay under the limit, and lose nothing`() {
        val text = "Flow: cold\nChannel: hot\n\nRoom: SQLite\n" + "x".repeat(50)
        assertEquals(listOf("Flow: cold\nChannel: hot", "Room: SQLite", "x".repeat(30), "x".repeat(20)), Chunker.chunks(text, maxChars = 30))
    }

    @Test
    fun `the prompt numbers the sources`() {
        assertEquals("[1] A\n[2] B\n\nQuestion: Q", Grounding.userPrompt("Q", listOf("A", "B")))
    }

    @Test
    fun `answers are grounded only if they cite real sources, or refuse`() {
        assertEquals(GroundedAnswer("Cold [1], hot [2].", listOf(1, 2), grounded = true), Grounding.check("Cold [1], hot [2].", sourceCount = 2))
        assertFalse(Grounding.check("Channels are hot.", sourceCount = 2).grounded)
        assertFalse(Grounding.check("See [3].", sourceCount = 2).grounded)
        assertTrue(Grounding.check(" Not in your notes. ", sourceCount = 2).grounded)
    }

    @Test
    fun `a plan is trimmed to the learner's decks, and nonsense is rejected`() {
        val snapshot = StudySnapshot(20, listOf(DeckStats("Kotlin basics", 4, 2, 10, 3)), List(7) { 0 })
        assertEquals(
            WeeklyPlan(25, listOf("Kotlin basics"), "More reviews"),
            PlanRules.validate(WeeklyPlan(25, listOf("Kotlin basics", "Made-up deck", "Kotlin basics"), " More reviews "), snapshot),
        )
        assertFailsWith<InvalidPlanException> { PlanRules.validate(WeeklyPlan(500, listOf("Kotlin basics"), "x"), snapshot) }
        assertFailsWith<InvalidPlanException> { PlanRules.validate(WeeklyPlan(20, listOf("Made-up deck"), "x"), snapshot) }
        assertFailsWith<InvalidPlanException> { PlanRules.validate(WeeklyPlan(20, listOf("Kotlin basics"), " "), snapshot) }
    }
}
