package dev.notedeck.domain

import dev.notedeck.model.Deck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface DeckRepository {
    val decks: Flow<List<Deck>>

    fun addDeck(deck: Deck)
}

class FakeDeckRepository : DeckRepository {
    private val state = MutableStateFlow(emptyList<Deck>())

    override val decks: Flow<List<Deck>> = state.asStateFlow()

    override fun addDeck(deck: Deck) {
        state.update { it + deck }
    }
}
