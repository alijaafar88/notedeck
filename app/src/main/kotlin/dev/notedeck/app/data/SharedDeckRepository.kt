package dev.notedeck.app.data

import dev.notedeck.app.network.NetworkResult
import dev.notedeck.app.network.SharedDecksApi
import dev.notedeck.app.network.safeCall
import dev.notedeck.domain.DeckRepository
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SharedDeckSummary(val id: String, val name: String, val cardCount: Int)

/**
 * Offline-first: the catalog on screen always comes from Room. The network only refreshes Room,
 * so with no connection the learner still sees the last catalog.
 */
@Singleton
class SharedDeckRepository @Inject constructor(
    private val api: SharedDecksApi,
    private val dao: SharedDeckDao,
    private val decks: DeckRepository,
    private val clock: Clock,
) {
    val catalog: Flow<List<SharedDeckSummary>> =
        dao.observeAll().map { rows -> rows.map { SharedDeckSummary(it.id, it.name, it.cardCount) } }

    /** Stale-while-revalidate: refresh only if the cached catalog is older than [maxAge]. */
    suspend fun refreshIfStale(maxAge: Duration = 30.minutes): NetworkResult<Unit> {
        val fetchedAt = dao.lastFetched() ?: Instant.DISTANT_PAST
        return if (clock.now() - fetchedAt > maxAge) refresh() else NetworkResult.Success(Unit)
    }

    suspend fun refresh(): NetworkResult<Unit> = safeCall {
        val now = clock.now()
        dao.replaceAll(api.index().map { SharedDeckEntity(it.id, it.name, it.cardCount, fetchedAt = now) })
    }

    /** Downloads one deck and adds it to the learner's own decks. Importing it again changes nothing: deck names are unique. */
    suspend fun import(id: String): NetworkResult<Unit> = safeCall {
        val shared = api.deck(id)
        decks.addDeck(Deck(shared.name, shared.cards.mapIndexed { index, card -> Card("${shared.id}-$index", card.front, card.back) }))
    }
}
