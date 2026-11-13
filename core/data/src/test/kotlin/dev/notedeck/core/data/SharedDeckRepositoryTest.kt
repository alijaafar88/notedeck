package dev.notedeck.core.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.notedeck.core.data.network.DeckSummary
import dev.notedeck.core.data.network.SharedCard
import dev.notedeck.core.data.network.SharedDeck
import dev.notedeck.core.data.network.SharedDecksApi
import dev.notedeck.domain.NetworkResult
import dev.notedeck.domain.SharedDeckSummary
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** A fake API is enough above the HTTP layer: it can be told to fail, and counts its calls. */
class FakeSharedDecksApi : SharedDecksApi {
    var index = listOf(DeckSummary("kotlin-collections", "Kotlin collections", 1))
    var offline = false
    var indexCalls = 0

    override suspend fun index(): List<DeckSummary> {
        indexCalls++
        if (offline) throw IOException("offline")
        return index
    }

    override suspend fun deck(id: String): SharedDeck {
        if (offline) throw IOException("offline")
        return SharedDeck(id, "Kotlin collections", listOf(SharedCard("What does map do?", "Transforms each element")))
    }
}

class MutableClock(var now: Instant) : Clock {
    override fun now() = now
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SharedDeckRepositoryTest {
    private lateinit var db: NoteDeckDatabase
    private val api = FakeSharedDecksApi()
    private val clock = MutableClock(Instant.parse("2026-11-12T09:00:00Z"))
    private lateinit var decks: RoomDeckRepository
    private lateinit var repository: SharedDeckRepository

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<NoteDeckDatabase>(RuntimeEnvironment.getApplication())
            .setDriver(AndroidSQLiteDriver())
            .build()
        decks = RoomDeckRepository(db.deckDao())
        repository = SharedDeckRepository(api, db.sharedDeckDao(), decks, clock)
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun `a refresh writes the catalog into Room`() = runTest {
        assertEquals(NetworkResult.Success(Unit), repository.refresh())
        assertEquals(listOf(SharedDeckSummary("kotlin-collections", "Kotlin collections", 1)), repository.catalog.first())
    }

    @Test
    fun `offline keeps the cached catalog`() = runTest {
        repository.refresh()
        api.offline = true
        assertEquals(NetworkResult.Offline, repository.refresh())
        assertEquals(listOf("Kotlin collections"), repository.catalog.first().map { it.name })
    }

    @Test
    fun `a fresh catalog isn't fetched again, a stale one is`() = runTest {
        repository.refreshIfStale()
        clock.now += 10.minutes
        repository.refreshIfStale()
        assertEquals(1, api.indexCalls)
        clock.now += 1.hours
        repository.refreshIfStale()
        assertEquals(2, api.indexCalls)
    }

    @Test
    fun `a deck removed from the server leaves the catalog`() = runTest {
        repository.refresh()
        api.index = emptyList()
        repository.refresh()
        assertEquals(emptyList(), repository.catalog.first())
    }

    @Test
    fun `importing adds the deck to your own decks, once`() = runTest {
        repository.import("kotlin-collections")
        repository.import("kotlin-collections")
        val imported = decks.decks.first().filter { it.name == "Kotlin collections" }
        assertEquals(1, imported.size)
        assertEquals(listOf("What does map do?"), imported.single().cards.map { it.front })
    }
}
