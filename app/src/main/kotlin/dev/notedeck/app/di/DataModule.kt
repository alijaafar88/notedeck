package dev.notedeck.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.notedeck.app.SampleDecks
import dev.notedeck.domain.DeckRepository
import dev.notedeck.domain.FakeDeckRepository
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** The in-memory repository from K4, filled with sample decks until Phase 4 adds Room. */
class SampleDeckRepository @Inject constructor() : DeckRepository by (FakeDeckRepository().apply { SampleDecks.all.forEach(::addDeck) })

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /** One repository for the whole app: it's the single source of truth. */
    @Binds
    @Singleton
    abstract fun bindDecks(impl: SampleDeckRepository): DeckRepository
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO
}
