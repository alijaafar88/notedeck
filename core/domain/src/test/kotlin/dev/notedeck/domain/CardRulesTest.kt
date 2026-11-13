package dev.notedeck.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class CardRulesTest {
    @Test
    fun `drafts are trimmed, and blanks, overlong cards, existing fronts and repeats are dropped`() {
        val drafts = listOf(
            CardDraft("  Is a Flow cold?  ", " Yes "),
            CardDraft("", "No front"),
            CardDraft("No back", "   "),
            CardDraft("Too long", "x".repeat(CardRules.MAX_BACK + 1)),
            CardDraft("What does Room store?", "Tables"),
            CardDraft("is a flow COLD?", "Repeat of the first"),
        )
        assertEquals(
            listOf(CardDraft("Is a Flow cold?", "Yes")),
            CardRules.clean(drafts, existingFronts = listOf("what does room store?")),
        )
    }

    @Test
    fun `a long back at the limit is kept`() {
        val atLimit = CardDraft("Q", "x".repeat(CardRules.MAX_BACK))
        assertEquals(listOf(atLimit), CardRules.clean(listOf(atLimit)))
    }

    @Test
    fun `at most MAX_CARDS drafts are kept`() {
        val many = (1..30).map { CardDraft("Q$it", "A") }
        assertEquals(CardRules.MAX_CARDS, CardRules.clean(many).size)
    }

    @Test
    fun `notes are delimited and capped`() {
        val prompt = CardRules.userPrompt("y".repeat(CardRules.MAX_NOTES + 500))
        assertTrue(prompt.startsWith("<notes>\n") && prompt.endsWith("\n</notes>"))
        assertEquals(CardRules.MAX_NOTES + "<notes>\n\n</notes>".length, prompt.length)
    }

    @Test
    fun `the fake turns term-definition lines into cards, streamed then done`() = runTest {
        val events = FakeCardGenerator().draft("Flow: a cold stream\nnot a card\nRoom - a database").toList()
        assertEquals(
            DraftEvent.Done(listOf(CardDraft("What is Flow?", "a cold stream"), CardDraft("What is Room?", "a database"))),
            events.last(),
        )
        assertEquals(2, events.count { it is DraftEvent.Text })
    }
}
