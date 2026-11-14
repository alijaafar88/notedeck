package dev.notedeck.domain

import kotlinx.coroutines.flow.Flow

enum class AiRating { Helpful, NotHelpful, Reported }

data class FeedbackCounts(val helpful: Int = 0, val notHelpful: Int = 0, val reported: Int = 0)

/**
 * Learners' ratings of AI results, kept on the phone with the prompt version that produced each result, so a
 * report points at what caused it. Nothing here is sent anywhere.
 */
interface FeedbackStore {
    val counts: Flow<FeedbackCounts>

    suspend fun record(feature: String, promptVersion: String, rating: AiRating)
}
