package dev.notedeck.core.data.ai

import dev.notedeck.domain.CardDraft
import dev.notedeck.domain.DraftEvent
import dev.notedeck.domain.InvalidReplyException
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient

/** Real HTTP against MockWebServer: the same bytes the backend writes, read the way the app reads them. */
class ClaudeCardGeneratorTest {
    private val server = MockWebServer()

    @BeforeTest
    fun setUp() = server.start()

    @AfterTest
    fun tearDown() = server.close()

    private fun generator(dispatcher: TestDispatcher, url: String? = server.url("/").toString()) =
        ClaudeCardGenerator(BackendClient(OkHttpClient(), AiConfig(url), dispatcher))

    /** One `data:` line, encoded the way the backend encodes it. */
    private fun data(piece: String) = "data: " + Json.encodeToString(piece)

    private fun sse(vararg lines: String) = MockResponse.Builder()
        .addHeader("Content-Type", "text/event-stream")
        .body(lines.joinToString("\n") + "\n")
        .build()

    @Test
    fun `text streams in, then the whole reply is parsed`() = runTest {
        server.enqueue(
            sse(
                data("""[{"front": "Is a Flow"""), "",
                ": keep-alive", "",
                data(""" cold?", "back": "Yes"}]"""), "",
                "event: done", "data: ", "",
            ),
        )
        val events = generator(StandardTestDispatcher(testScheduler)).draft("Flows are cold").toList()
        assertEquals(
            listOf(
                DraftEvent.Text("""[{"front": "Is a Flow"""),
                DraftEvent.Text(""" cold?", "back": "Yes"}]"""),
                DraftEvent.Done(listOf(CardDraft("Is a Flow cold?", "Yes"))),
            ),
            events,
        )
        val request = server.takeRequest()
        assertEquals("/cards", request.url.encodedPath)
        assertEquals("Flows are cold", request.body?.utf8())
    }

    @Test
    fun `an error event becomes an exception with the server's message`() = runTest {
        server.enqueue(sse("event: error", data("Claude is busy. Try again shortly."), ""))
        val error = assertFailsWith<IOException> { generator(StandardTestDispatcher(testScheduler)).draft("notes").toList() }
        assertEquals("Claude is busy. Try again shortly.", error.message)
    }

    @Test
    fun `a 429 explains the rate limit`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).build())
        val error = assertFailsWith<IOException> { generator(StandardTestDispatcher(testScheduler)).draft("notes").toList() }
        assertEquals(BackendClient.messageFor(429), error.message)
    }

    @Test
    fun `a stream that ends without done is an error, not a silent partial reply`() = runTest {
        server.enqueue(sse(data("""[{"front""""), ""))
        assertFailsWith<IOException> { generator(StandardTestDispatcher(testScheduler)).draft("notes").toList() }
    }

    @Test
    fun `a reply that isn't a card list is an invalid reply`() = runTest {
        server.enqueue(sse(data("Sure! Here are some cards."), "", "event: done", "data: ", ""))
        assertFailsWith<InvalidReplyException> { generator(StandardTestDispatcher(testScheduler)).draft("notes").toList() }
    }

    @Test
    fun `without a backend URL, Claude is unavailable`() = runTest {
        assertFalse(generator(StandardTestDispatcher(testScheduler), url = null).available)
    }
}
