package dev.notedeck.core.data.ai

import dev.notedeck.domain.CardDraft
import dev.notedeck.domain.DraftEvent
import dev.notedeck.domain.InvalidReplyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Turns a model's complete reply into drafts. Strict: a JSON array of {front, back}, optionally in a code fence. */
object CardReplies {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Card(val front: String = "", val back: String = "")

    fun parse(reply: String): List<CardDraft> {
        val body = reply.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        if (!body.startsWith("[")) throw InvalidReplyException("The model didn't reply with a list of cards. Try again.")
        val cards = runCatching { json.decodeFromString<List<Card>>(body) }
            .getOrElse { throw InvalidReplyException("The model's reply was cut off or malformed. Try again.") }
        return cards.map { CardDraft(it.front, it.back) }
    }
}

/** Passes each text piece through as it arrives, then parses the whole reply once the stream completes. */
fun Flow<String>.toDraftEvents(): Flow<DraftEvent> = flow {
    val reply = StringBuilder()
    collect { piece ->
        reply.append(piece)
        emit(DraftEvent.Text(piece))
    }
    emit(DraftEvent.Done(CardReplies.parse(reply.toString())))
}
