package dev.notedeck.domain

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** What to ask a model. Plain Kotlin, so every backend (Gemini, Claude, Gemini Nano, a fake) can take it. */
data class TextRequest(val prompt: String, val temperature: Float = 0f, val system: String? = null)

/** A text model behind NoteDeck's own interface. No SDK types leak out, so features and tests never see one. */
interface TextModel {
    /** Shown in the UI, for example "Gemini" or "Fake". */
    val name: String

    /** Streams the reply in pieces. Cancelling the collector stops generation. */
    fun generate(request: TextRequest): Flow<String>
}

/**
 * Stands in for a real model in tests, previews and the Playground. The same reply every time at
 * temperature 0, a different opening above it, streamed a word at a time.
 */
class FakeTextModel(
    private val delayPerWord: Duration = 40.milliseconds,
    private val random: Random = Random(seed = 42),
) : TextModel {
    override val name = "Fake"

    override fun generate(request: TextRequest): Flow<String> = flow {
        reply(request).split(" ").forEachIndexed { index, word ->
            delay(delayPerWord)
            emit(if (index == 0) word else " $word")
        }
    }

    fun reply(request: TextRequest, opening: String = opening(request.temperature)): String =
        "$opening Front: \"${request.prompt.trim().take(60)}?\" Back: what your notes say. (A real model replaces this in AI3.)"

    private fun opening(temperature: Float): String =
        if (temperature == 0f) OPENINGS.first() else OPENINGS[random.nextInt(OPENINGS.size)]

    companion object {
        val OPENINGS = listOf("Here's a card.", "Try this one.", "One idea:", "How about:")
    }
}
