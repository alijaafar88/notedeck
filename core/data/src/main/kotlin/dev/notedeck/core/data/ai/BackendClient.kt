package dev.notedeck.core.data.ai

import dev.notedeck.domain.IoDispatcher
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Where NoteDeck's own backend runs. Null when there isn't one, and then the cloud features show as unavailable. */
data class AiConfig(val proxyUrl: String?)

/**
 * NoteDeck's backend (backend/), which holds the Anthropic key. Every route streams server-sent events:
 * `data:` lines holding JSON strings, then `event: done`, or `event: error` with a message for the learner.
 */
class BackendClient @Inject constructor(
    client: OkHttpClient,
    private val config: AiConfig,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val configured: Boolean get() = config.proxyUrl != null

    // The shared client gives up after 30 seconds; a long reply can stream for longer.
    private val streaming = client.newBuilder().readTimeout(90, TimeUnit.SECONDS).callTimeout(0, TimeUnit.SECONDS).build()

    fun stream(path: String, text: String, type: MediaType = TEXT): Flow<String> = flow {
        val url = config.proxyUrl ?: throw IllegalStateException("No backend is set up.")
        val request = Request.Builder()
            .url(url.trimEnd('/') + path)
            .post(text.toRequestBody(type))
            .build()
        val call = streaming.newCall(request)
        // Leaving the screen cancels the collector; cancel the HTTP call with it, so the read stops at once.
        currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        call.execute().use { response ->
            if (!response.isSuccessful) throw IOException(messageFor(response.code))
            val source = response.body.source()
            var event = "message"
            while (true) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.isEmpty() -> event = "message"
                    line.startsWith(":") -> Unit // a comment, such as a keep-alive
                    line.startsWith("event:") -> event = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> {
                        val data = line.removePrefix("data:").trim()
                        when (event) {
                            "done" -> return@use
                            "error" -> throw IOException(Json.decodeFromString<String>(data))
                            else -> emit(Json.decodeFromString<String>(data))
                        }
                    }
                }
            }
            throw IOException("The connection closed before the reply finished.")
        }
    }.flowOn(io)

    /** A plain request and response, for routes that don't stream (the coach's agent answers once, at the end). */
    suspend fun post(path: String, body: String, type: MediaType = JSON): String = withContext(io) {
        val url = config.proxyUrl ?: throw IllegalStateException("No backend is set up.")
        val request = Request.Builder().url(url.trimEnd('/') + path).post(body.toRequestBody(type)).build()
        streaming.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException(messageFor(response.code))
            response.body.string()
        }
    }

    companion object {
        val TEXT = "text/plain; charset=utf-8".toMediaType()
        val JSON = "application/json; charset=utf-8".toMediaType()

        fun messageFor(code: Int): String = when (code) {
            400 -> "Paste some notes first."
            413 -> "These notes are too long. Try a shorter part."
            429 -> "Too many requests. Wait a minute and try again."
            else -> "The AI service isn't working right now ($code). Try again later."
        }
    }
}
