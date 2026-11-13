package dev.notedeck.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.notedeck.app.BuildConfig
import dev.notedeck.app.data.NoteDeckDatabase
import dev.notedeck.app.data.SharedDeckDao
import dev.notedeck.app.network.ConnectivityNetworkMonitor
import dev.notedeck.app.network.NetworkMonitor
import dev.notedeck.app.network.SharedDecksApi
import dev.notedeck.app.work.AndroidReminderNotifier
import dev.notedeck.app.work.DueCardCounter
import dev.notedeck.app.work.ReminderNotifier
import dev.notedeck.app.work.RoomDueCardCounter
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** The companion repo's GitHub Pages site. Public JSON, so no key is needed, and none would belong here anyway. */
const val SHARED_DECKS_BASE_URL = "https://alijaafar88.github.io/notedeck/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** One client for the whole app: it pools connections. Logs only in debug builds. */
    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun sharedDecksApi(client: OkHttpClient): SharedDecksApi = sharedDecksApi(SHARED_DECKS_BASE_URL, client)

    @Provides
    fun sharedDeckDao(database: NoteDeckDatabase): SharedDeckDao = database.sharedDeckDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule {
    @Binds
    abstract fun bindMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    abstract fun bindDueCards(impl: RoomDueCardCounter): DueCardCounter

    @Binds
    abstract fun bindNotifier(impl: AndroidReminderNotifier): ReminderNotifier
}

/** The server may add fields; app versions already installed must keep working. */
private val json = Json { ignoreUnknownKeys = true }

/** Also used by the tests, pointed at MockWebServer. */
fun sharedDecksApi(baseUrl: String, client: OkHttpClient = OkHttpClient()): SharedDecksApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SharedDecksApi::class.java)
