package dev.notedeck.cli

import dev.notedeck.domain.DeckRepository
import dev.notedeck.domain.FakeDeckRepository
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val repository: DeckRepository = FakeDeckRepository()
    val printer = launch {
        repository.decks.collect { decks -> println("Decks: ${decks.map { it.name }}") }
    }

    delay(UPDATE_GAP_MS)
    repository.addDeck(Deck("Kotlin basics", listOf(Card("1", "What makes a type nullable?", "A trailing ?"))))
    delay(UPDATE_GAP_MS)
    repository.addDeck(Deck("Coroutines", emptyList()))
    delay(UPDATE_GAP_MS)
    printer.cancel()
}

// StateFlow keeps only the latest value, so give the collector a moment to print each one.
private const val UPDATE_GAP_MS = 50L
