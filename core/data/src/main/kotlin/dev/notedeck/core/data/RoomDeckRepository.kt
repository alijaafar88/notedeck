package dev.notedeck.core.data

import dev.notedeck.domain.DeckRepository
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import dev.notedeck.model.Review
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * The single source of truth: every read comes from Room and every write goes to Room.
 * Callers never patch their own copies; the Flow re-emits after each write.
 */
class RoomDeckRepository @Inject constructor(private val dao: DeckDao) : DeckRepository {
    override val decks: Flow<List<Deck>> = dao.observeDecksWithCards()
        .onStart { seedIfEmpty() }
        .map { rows -> rows.map { it.toDomain() } }

    override suspend fun addDeck(deck: Deck) {
        dao.insertDeckWithCards(DeckEntity(name = deck.name), deck.cards.map { it.toEntity(deckId = 0, position = 0) })
    }

    override suspend fun addCard(deckName: String, card: Card) {
        val deckId = dao.deckId(deckName) ?: return
        dao.upsertCards(listOf(card.toEntity(deckId, position = dao.cardCount(deckId))))
    }

    override suspend fun recordReview(review: Review) {
        dao.insertReview(review.toEntity())
    }

    /** First launch: sample decks, so the app isn't empty. Deck names are unique, so a second attempt writes nothing. */
    private suspend fun seedIfEmpty() {
        if (dao.deckCount() == 0) SampleDecks.all.forEach { addDeck(it) }
    }
}
