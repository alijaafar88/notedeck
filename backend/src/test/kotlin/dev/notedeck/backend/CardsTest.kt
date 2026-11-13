package dev.notedeck.backend

import dev.notedeck.domain.CardRules
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** The whole route, in memory, with a fake Claude: no key, no network, no cost. */
class CardsTest {
    private val received = mutableListOf<Pair<String, String>>()

    private fun recording(vararg pieces: String) = TextStreamer { system, user ->
        received += system to user
        flowOf(*pieces)
    }

    @Test
    fun `the reply streams as JSON-encoded events, then done`() = testApplication {
        application { cardsModule(recording("[{\"front\": \"Q\",\n", " \"back\": \"A\"}]"), RateLimiter(perMinute = 10)) }
        val response = client.post("/cards") { setBody("Flows are cold") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.contentType()!!.match(ContentType.Text.EventStream))
        assertEquals(
            "data: \"[{\\\"front\\\": \\\"Q\\\",\\n\"\n\n" +
                "data: \" \\\"back\\\": \\\"A\\\"}]\"\n\n" +
                "event: done\ndata: \n\n",
            response.bodyAsText(),
        )
        assertEquals(CardRules.SYSTEM_PROMPT to CardRules.userPrompt("Flows are cold"), received.single())
    }

    @Test
    fun `blank or oversized notes are refused before calling the model`() = testApplication {
        application { cardsModule(recording("x"), RateLimiter(perMinute = 10)) }
        assertEquals(HttpStatusCode.BadRequest, client.post("/cards") { setBody("  ") }.status)
        assertEquals(HttpStatusCode.PayloadTooLarge, client.post("/cards") { setBody("x".repeat(CardRules.MAX_NOTES + 1)) }.status)
        assertEquals(emptyList(), received)
    }

    @Test
    fun `a busy model becomes an error event with a message the learner can read`() = testApplication {
        val failing = TextStreamer { _, _ -> flow { throw StreamFailure("Claude is busy. Try again shortly.", retryable = true) } }
        application { cardsModule(failing, RateLimiter(perMinute = 10)) }
        assertEquals("event: error\ndata: \"Claude is busy. Try again shortly.\"\n\n", client.post("/cards") { setBody("notes") }.bodyAsText())
    }

    @Test
    fun `an unexpected failure doesn't leak its details`() = testApplication {
        val failing = TextStreamer { _, _ -> flow { throw IllegalStateException("secret internal detail") } }
        application { cardsModule(failing, RateLimiter(perMinute = 10)) }
        val body = client.post("/cards") { setBody("notes") }.bodyAsText()
        assertTrue("secret" !in body && "event: error" in body, body)
    }

    @Test
    fun `the eleventh request in a minute is rate limited`() = testApplication {
        application { cardsModule(recording("[]"), RateLimiter(perMinute = 10)) }
        repeat(10) { assertEquals(HttpStatusCode.OK, client.post("/cards") { setBody("notes") }.status) }
        assertEquals(HttpStatusCode.TooManyRequests, client.post("/cards") { setBody("notes") }.status)
    }

    @Test
    fun `the limiter forgets requests older than a minute`() {
        var now = 0L
        val limiter = RateLimiter(perMinute = 2) { now }
        assertTrue(limiter.tryAcquire("a") && limiter.tryAcquire("a"))
        assertTrue(!limiter.tryAcquire("a"))
        assertTrue(limiter.tryAcquire("b"), "each client has its own budget")
        now = 60_001
        assertTrue(limiter.tryAcquire("a"))
    }
}
