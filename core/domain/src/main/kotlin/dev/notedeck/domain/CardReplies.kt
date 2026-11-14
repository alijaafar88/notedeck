package dev.notedeck.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Turns a model's complete reply into drafts. Strict: a JSON array of {front, back}, optionally in a code fence.
 * One copy, used by the app, the backend and the evals, so what CI checks is what the app accepts.
 */
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
