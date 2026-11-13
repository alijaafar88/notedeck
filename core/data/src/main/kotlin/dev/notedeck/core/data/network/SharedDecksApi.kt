package dev.notedeck.core.data.network

import dev.notedeck.domain.NetworkResult
import java.io.IOException
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path

/** One entry in decks/index.json. */
@Serializable
data class DeckSummary(val id: String, val name: String, val cardCount: Int = 0)

@Serializable
data class SharedCard(val front: String, val back: String)

/** One file in decks/, such as decks/kotlin-collections.json. */
@Serializable
data class SharedDeck(val id: String, val name: String, val cards: List<SharedCard> = emptyList())

/** Static JSON served by GitHub Pages from the companion repo's decks/ folder. */
interface SharedDecksApi {
    @GET("decks/index.json")
    suspend fun index(): List<DeckSummary>

    @GET("decks/{id}.json")
    suspend fun deck(@Path("id") id: String): SharedDeck
}

/** Catches only what the network throws. CancellationException must keep propagating, so no catch-all. */
suspend fun <T> safeCall(block: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(block())
    } catch (e: HttpException) {
        NetworkResult.HttpError(e.code())
    } catch (e: IOException) {
        NetworkResult.Offline
    }
