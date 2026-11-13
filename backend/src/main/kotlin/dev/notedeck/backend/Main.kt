package dev.notedeck.backend

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.messages.MessageCreateParams
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

fun main() {
    // Reads ANTHROPIC_API_KEY from this process's environment. The app never sees it.
    val model = System.getenv("CLAUDE_MODEL") ?: DEFAULT_MODEL
    val claude = ClaudeStreamer(AnthropicOkHttpClient.fromEnv(), model)
    val coach = KoogCoach(apiKey = requireNotNull(System.getenv("ANTHROPIC_API_KEY")) { "Set ANTHROPIC_API_KEY" }, model = model)
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) { cardsModule(claude, RateLimiter(perMinute = 10), coach) }.start(wait = true)
}

const val DEFAULT_MODEL = "claude-opus-5-5"

/** Claude through the Anthropic Java SDK, streamed. Current Claude models pick their own sampling, so there's no temperature. */
class ClaudeStreamer(private val client: AnthropicClient, private val model: String) : TextStreamer {
    override fun stream(system: String, user: String): Flow<String> = flow {
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(16_000L)
            .system(system)
            .addUserMessage(user)
            .build()
        try {
            client.messages().createStreaming(params).use { response ->
                for (event in response.stream().iterator()) {
                    val text = event.contentBlockDelta().flatMap { it.delta().text() }.map { it.text() }.orElse(null) ?: continue
                    emit(text)
                }
            }
        } catch (e: AnthropicServiceException) {
            // 429 rate limited and 529 overloaded pass with time (the SDK has already retried); others need a fix here.
            val busy = e.statusCode() == 429 || e.statusCode() == 529
            throw StreamFailure(if (busy) "Claude is busy. Try again shortly." else "The card service failed (${e.statusCode()}).", retryable = busy)
        }
    }.flowOn(Dispatchers.IO)
}
