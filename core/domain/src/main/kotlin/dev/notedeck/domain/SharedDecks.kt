package dev.notedeck.domain

import kotlinx.coroutines.flow.Flow

/** Every outcome of a network call, so callers handle each one and the compiler checks it. */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>

    data class HttpError(val code: Int) : NetworkResult<Nothing>

    data object Offline : NetworkResult<Nothing>
}

data class SharedDeckSummary(val id: String, val name: String, val cardCount: Int)

/** The shared-deck catalog, as features see it: read from the local cache, refreshed from the network. */
interface SharedDeckCatalog {
    val catalog: Flow<List<SharedDeckSummary>>

    suspend fun refreshIfStale(): NetworkResult<Unit>

    suspend fun refresh(): NetworkResult<Unit>

    /** Downloads one deck and adds it to the learner's own decks. */
    suspend fun import(id: String): NetworkResult<Unit>
}

interface NetworkMonitor {
    /** True while the phone has a validated internet connection. */
    val isOnline: Flow<Boolean>
}
