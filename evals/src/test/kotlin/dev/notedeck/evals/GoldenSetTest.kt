package dev.notedeck.evals

import dev.notedeck.domain.CardRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The committed data itself: well-formed, complete, and passing the gate with the current prompt version. */
class GoldenSetTest {
    private val golden = Evals.loadGolden()
    private val replies = Evals.loadReplies()

    @Test
    fun `thirty cases, unique ids, three of them injection attempts and two with no facts`() {
        assertEquals(30, golden.cases.size)
        assertEquals(golden.cases.size, golden.cases.map { it.id }.toSet().size)
        assertEquals(3, golden.cases.count { it.injection })
        assertEquals(2, golden.cases.count { it.expectEmpty })
        assertTrue(golden.cases.filter { it.injection }.all { it.mustNotMention.isNotEmpty() })
    }

    @Test
    fun `every case has a reply, recorded for the current prompt`() {
        assertEquals(golden.cases.map { it.id }.toSet(), replies.replies.keys)
        assertEquals(CardRules.PROMPT_VERSION, replies.promptVersion, "re-record the replies after changing the prompt")
    }

    @Test
    fun `the stored replies meet the gate`() {
        val report = Evals.run(golden, replies.replies, "test")
        assertTrue(report.ok, report.format())
        assertEquals(30, report.passed)
    }
}
