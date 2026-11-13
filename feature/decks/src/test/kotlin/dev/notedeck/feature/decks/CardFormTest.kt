package dev.notedeck.feature.decks

import kotlin.test.Test
import kotlin.test.assertEquals

class CardFormTest {
    @Test
    fun `both fields are needed`() {
        listOf(
            Triple("val?", "read-only", true),
            Triple("", "read-only", false),
            Triple("val?", "  ", false),
            Triple("", "", false),
        ).forEach { (front, back, expected) ->
            assertEquals(expected, canSave(front, back), "'$front' / '$back'")
        }
    }
}
