package dev.notedeck.app.nav

import kotlin.test.Test
import kotlin.test.assertEquals

class TabStacksTest {
    private fun tabs() = TabStacks(listOf("Decks", "Study", "Settings")) { listOf("Decks", "Study", "Settings")[it] }

    @Test
    fun `back pops within a tab, then returns to the first tab, then leaves`() {
        val tabs = tabs()
        tabs.current += "Deck:Kotlin"
        tabs.select(1)
        tabs.current += "Study:Kotlin"
        assertEquals(true, tabs.back())
        assertEquals("Study", tabs.current.last())
        assertEquals(true, tabs.back())
        assertEquals(0, tabs.selected)
        assertEquals("Deck:Kotlin", tabs.current.last())
        assertEquals(true, tabs.back())
        assertEquals(false, tabs.back())
    }

    @Test
    fun `switching tabs keeps each tab's history`() {
        val tabs = tabs()
        tabs.current += "Deck:Kotlin"
        tabs.select(2)
        tabs.select(0)
        assertEquals(listOf("Decks", "Deck:Kotlin"), tabs.current)
    }
}
