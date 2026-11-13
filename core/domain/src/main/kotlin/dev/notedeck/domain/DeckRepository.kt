package dev.notedeck.domain

import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import dev.notedeck.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The app's decks. Reads are a Flow; writes suspend, because a real implementation writes to a database. */
interface DeckRepository {
    val decks: Flow<List<Deck>>

    suspend fun addDeck(deck: Deck)

    /** Adds [card] to the deck named [deckName]; does nothing if there's no such deck. */
    suspend fun addCard(deckName: String, card: Card)

    suspend fun recordReview(review: Review)
}

/** In memory, for tests and the console app. */
class FakeDeckRepository(initial: List<Deck> = emptyList()) : DeckRepository {
    private val state = MutableStateFlow(initial)
    private val recorded = mutableListOf<Review>()

    override val decks: Flow<List<Deck>> = state.asStateFlow()

    /** Every review recorded so far, oldest first. */
    val reviews: List<Review> get() = recorded.toList()

    override suspend fun addDeck(deck: Deck) {
        state.update { it + deck }
    }

    override suspend fun addCard(deckName: String, card: Card) {
        state.update { decks -> decks.map { if (it.name == deckName) it.copy(cards = it.cards + card) else it } }
    }

    override suspend fun recordReview(review: Review) {
        recorded += review
    }
}
