package dev.notedeck.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CardRepliesTest {
    @Test
    fun `a JSON array of cards parses, ignoring extra fields`() {
        assertEquals(
            listOf(CardDraft("Q", "A")),
            CardReplies.parse("""[{"front": "Q", "back": "A", "confidence": 0.9}]"""),
        )
    }

    @Test
    fun `a code fence around the array is allowed`() {
        assertEquals(listOf(CardDraft("Q", "A")), CardReplies.parse("```json\n[{\"front\": \"Q\", \"back\": \"A\"}]\n```"))
    }

    @Test
    fun `an empty array is a valid answer`() {
        assertEquals(emptyList(), CardReplies.parse("[]"))
    }

    @Test
    fun `chatty text, a cut-off array or the wrong shape is rejected`() {
        listOf(
            """Here you go: [{"front": "Q", "back": "A"}]""",
            """[{"front": "Q", "back": "A"""",
            """[1, 2, 3]""",
        ).forEach { reply -> assertFailsWith<InvalidReplyException>(reply) { CardReplies.parse(reply) } }
    }
}
