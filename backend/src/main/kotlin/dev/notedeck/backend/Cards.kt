package dev.notedeck.backend

import dev.notedeck.domain.CardRules
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/** Streams a model's reply as text pieces. Claude in production; a fake in tests, so they need no key or network. */
fun interface TextStreamer {
    fun stream(system: String, user: String): Flow<String>
}

/** A failure the learner may see, with whether trying again later could help. */
class StreamFailure(message: String, val retryable: Boolean) : Exception(message)

/**
 * POST /cards: plain-text notes in, server-sent events out. Each `data:` line holds one JSON-encoded piece of
 * the reply; the stream ends with `event: done`, or `event: error` and a message.
 */
fun Application.cardsModule(streamer: TextStreamer, limiter: RateLimiter) {
    routing {
        get("/health") { call.respondText("ok") }

        post("/cards") {
            val client = call.request.origin.remoteAddress
            if (!limiter.tryAcquire(client)) {
                call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
                return@post
            }
            val notes = call.receiveText()
            when {
                notes.isBlank() -> call.respondText("No notes", status = HttpStatusCode.BadRequest)
                notes.length > CardRules.MAX_NOTES -> call.respondText("Notes too long", status = HttpStatusCode.PayloadTooLarge)
                else -> call.respondTextWriter(ContentType.Text.EventStream) {
                    try {
                        streamer.stream(CardRules.SYSTEM_PROMPT, CardRules.userPrompt(notes)).collect { piece ->
                            write("data: ${Json.encodeToString(piece)}\n\n")
                            flush()
                        }
                        write("event: done\ndata: \n\n")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val message = (e as? StreamFailure)?.message ?: "The card service failed. Try again later."
                        write("event: error\ndata: ${Json.encodeToString(message)}\n\n")
                    }
                    flush()
                }
            }
        }
    }
}

/** At most [perMinute] requests per client in any 60-second window. In memory: enough for one small server. */
class RateLimiter(private val perMinute: Int, private val now: () -> Long = System::currentTimeMillis) {
    private val recent = mutableMapOf<String, ArrayDeque<Long>>()

    @Synchronized
    fun tryAcquire(client: String): Boolean {
        val times = recent.getOrPut(client) { ArrayDeque() }
        val cutoff = now() - 60_000
        while (times.isNotEmpty() && times.first() <= cutoff) times.removeFirst()
        if (times.size >= perMinute) return false
        times.addLast(now())
        return true
    }
}
