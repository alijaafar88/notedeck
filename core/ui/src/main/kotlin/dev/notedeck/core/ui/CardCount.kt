package dev.notedeck.core.ui

/** "1 card", "2 cards". */
fun cardCount(count: Int): String = if (count == 1) "1 card" else "$count cards"
