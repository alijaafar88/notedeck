package dev.notedeck.backend

import dev.notedeck.domain.CardRules
import dev.notedeck.domain.Grounding
import dev.notedeck.domain.SummaryRules
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Streams a model's reply as text pieces. Claude in production; a fake in tests, so they need no key or network. */
fun interface TextStreamer {
    fun stream(system: String, user: String): Flow<String>
}

/** A failure the learner may see, with whether trying again later could help. */
class StreamFailure(message: String, val retryable: Boolean) : Exception(message)

/** The request can't be served as sent; the message goes back with the status. */
class BadInput(val status: HttpStatusCode, message: String) : Exception(message)

@Serializable
data class AnswerRequest(val question: String, val sources: List<String>)

/**
 * Requests in, server-sent events out. Each `data:` line holds one JSON-encoded piece of the reply; the stream
 * ends with `event: done`, or `event: error` and a message. The system prompts are fixed here, on the server.
 *   POST /cards    notes (text) -> flashcards as a JSON array
 *   POST /summary  notes (text) -> a short summary
 *   POST /answer   {question, sources} (JSON) -> an answer citing the sources like [1]
 *   POST /coach    a statistics snapshot (JSON) -> a proposed plan (JSON, not streamed); see Coach.kt
 */
fun Application.cardsModule(streamer: TextStreamer, limiter: RateLimiter, coach: CoachAgent = CoachAgent { error("No coach configured") }) {
    routing {
        get("/health") { call.respondText("ok") }
        streamingRoute("/cards", streamer, limiter, CardRules.SYSTEM_PROMPT) { CardRules.userPrompt(notes(it)) }
        streamingRoute("/summary", streamer, limiter, SummaryRules.SYSTEM_PROMPT) { CardRules.userPrompt(notes(it)) }
        streamingRoute("/answer", streamer, limiter, Grounding.SYSTEM_PROMPT) { body ->
            val request = runCatching { Json.decodeFromString<AnswerRequest>(body) }
                .getOrElse { throw BadInput(HttpStatusCode.BadRequest, "Expected a question and sources") }
            if (request.question.isBlank() || request.sources.isEmpty() || request.sources.size > 8) {
                throw BadInput(HttpStatusCode.BadRequest, "Send a question and one to eight sources")
            }
            if (request.question.length + request.sources.sumOf { it.length } > CardRules.MAX_NOTES) {
                throw BadInput(HttpStatusCode.PayloadTooLarge, "Too much text")
            }
            Grounding.userPrompt(request.question, request.sources)
        }
        coachRoute(coach, limiter)
    }
}

private fun notes(body: String): String {
    if (body.isBlank()) throw BadInput(HttpStatusCode.BadRequest, "No notes")
    if (body.length > CardRules.MAX_NOTES) throw BadInput(HttpStatusCode.PayloadTooLarge, "Notes too long")
    return body
}

/** Checks the caller's budget, builds the user prompt from the body, then streams one model call. */
private fun Routing.streamingRoute(path: String, streamer: TextStreamer, limiter: RateLimiter, system: String, userPrompt: (String) -> String) {
    post(path) {
        if (!limiter.tryAcquire(call.request.origin.remoteAddress)) {
            call.respondText("Too many requests", status = HttpStatusCode.TooManyRequests)
            return@post
        }
        val user = try {
            userPrompt(call.receiveText())
        } catch (e: BadInput) {
            call.respondText(e.message.orEmpty(), status = e.status)
            return@post
        }
        call.respondTextWriter(ContentType.Text.EventStream) {
            try {
                streamer.stream(system, user).collect { piece ->
                    write("data: ${Json.encodeToString(piece)}\n\n")
                    flush()
                }
                write("event: done\ndata: \n\n")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = (e as? StreamFailure)?.message ?: "The AI service failed. Try again later."
                write("event: error\ndata: ${Json.encodeToString(message)}\n\n")
            }
            flush()
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
