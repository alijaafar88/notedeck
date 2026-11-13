package dev.notedeck.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.notedeck.app.data.DeckDao
import dev.notedeck.app.data.NoteDeckDatabase
import dev.notedeck.app.data.RoomDeckRepository
import dev.notedeck.domain.DeckRepository
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /** One repository for the whole app. Room replaced the in-memory sample repository here, and nothing else changed. */
    @Binds
    @Singleton
    abstract fun bindDecks(impl: RoomDeckRepository): DeckRepository
}

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    /** Build the database once: it's expensive, and two instances would each cache their own view. */
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): NoteDeckDatabase =
        Room.databaseBuilder<NoteDeckDatabase>(context, "notedeck.db")
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Provides
    fun deckDao(database: NoteDeckDatabase): DeckDao = database.deckDao()

    /** Configured by NoteDeckApplication (Hilt's worker factory) the first time it's asked for. */
    @Provides
    fun workManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    /** DataStore needs exactly one instance per file. */
    @Provides
    @Singleton
    fun settingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
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

    /** Injected, so tests can stamp reviews with a fixed time. */
    @Provides
    fun clock(): Clock = Clock.System
}
