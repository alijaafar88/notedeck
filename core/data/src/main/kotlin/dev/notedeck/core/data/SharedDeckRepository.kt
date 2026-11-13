package dev.notedeck.core.data

import dev.notedeck.core.data.network.SharedDecksApi
import dev.notedeck.core.data.network.safeCall
import dev.notedeck.domain.DeckRepository
import dev.notedeck.domain.NetworkResult
import dev.notedeck.domain.SharedDeckCatalog
import dev.notedeck.domain.SharedDeckSummary
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
) : SharedDeckCatalog {
    override val catalog: Flow<List<SharedDeckSummary>> =
        dao.observeAll().map { rows -> rows.map { SharedDeckSummary(it.id, it.name, it.cardCount) } }

    override suspend fun refreshIfStale(): NetworkResult<Unit> = refreshIfStale(30.minutes)

    /** Stale-while-revalidate: refresh only if the cached catalog is older than [maxAge]. */
    suspend fun refreshIfStale(maxAge: Duration): NetworkResult<Unit> {
        val fetchedAt = dao.lastFetched() ?: Instant.DISTANT_PAST
        return if (clock.now() - fetchedAt > maxAge) refresh() else NetworkResult.Success(Unit)
    }

    override suspend fun refresh(): NetworkResult<Unit> = safeCall {
        val now = clock.now()
        dao.replaceAll(api.index().map { SharedDeckEntity(it.id, it.name, it.cardCount, fetchedAt = now) })
    }

    /** Downloads one deck and adds it to the learner's own decks. Importing it again changes nothing: deck names are unique. */
    override suspend fun import(id: String): NetworkResult<Unit> = safeCall {
        val shared = api.deck(id)
        decks.addDeck(Deck(shared.name, shared.cards.mapIndexed { index, card -> Card("${shared.id}-$index", card.front, card.back) }))
    }
}
