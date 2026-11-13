package dev.notedeck.app.network

import dev.notedeck.app.di.sharedDecksApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer

/** Real HTTP and real JSON: catches what a fake API can't, like a key that doesn't match the server's. */
class SharedDecksApiTest {
    private val server = MockWebServer()
    private lateinit var api: SharedDecksApi

    @BeforeTest
    fun setUp() {
        server.start()
        api = sharedDecksApi(server.url("/").toString())
    }

    @AfterTest
    fun tearDown() = server.close()

    @Test
    fun `parses the index from the decks folder`() = runTest {
        server.enqueue(MockResponse.Builder().body("""[{"id": "kotlin-collections", "name": "Kotlin collections", "cardCount": 4}]""").build())
        assertEquals(listOf(DeckSummary("kotlin-collections", "Kotlin collections", 4)), api.index())
        assertEquals("/decks/index.json", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `fields the server adds or leaves out don't break decoding`() = runTest {
        server.enqueue(MockResponse.Builder().body("""[{"id": "a", "name": "A", "author": "Ana"}]""").build())
        assertEquals(listOf(DeckSummary("a", "A", cardCount = 0)), api.index())
    }

    @Test
    fun `a deck is fetched by id`() = runTest {
        server.enqueue(MockResponse.Builder().body("""{"id": "a", "name": "A", "cards": [{"front": "Q", "back": "A"}]}""").build())
        assertEquals(SharedDeck("a", "A", listOf(SharedCard("Q", "A"))), api.deck("a"))
        assertEquals("/decks/a.json", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `an error status maps to HttpError`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).build())
        assertEquals(NetworkResult.HttpError(500), safeCall { api.index() })
    }

    @Test
    fun `no server maps to Offline`() = runTest {
        server.close()
        assertEquals(NetworkResult.Offline, safeCall { api.index() })
    }
}
