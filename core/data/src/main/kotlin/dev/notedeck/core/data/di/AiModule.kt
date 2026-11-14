package dev.notedeck.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.notedeck.core.data.BuildConfig
import dev.notedeck.core.data.FeedbackDao
import dev.notedeck.core.data.NoteDao
import dev.notedeck.core.data.NoteDeckDatabase
import dev.notedeck.core.data.SettingsRepository
import dev.notedeck.core.data.ai.BackendAnswerer
import dev.notedeck.core.data.ai.BackendCoach
import dev.notedeck.core.data.ai.ClaudeCardGenerator
import dev.notedeck.core.data.ai.CloudSummarizer
import dev.notedeck.core.data.ai.GeminiCardGenerator
import dev.notedeck.core.data.ai.MlKitOcrScanner
import dev.notedeck.core.data.ai.MlKitSummarizer
import dev.notedeck.core.data.ai.RoomFeedbackStore
import dev.notedeck.core.data.ai.RoomNoteIndex
import dev.notedeck.core.data.ai.RoomStatsSource
import dev.notedeck.core.data.ai.SettingsPlanStore
import dev.notedeck.domain.Answerer
import dev.notedeck.domain.CardGenerators
import dev.notedeck.domain.Coach
import dev.notedeck.domain.Embedder
import dev.notedeck.domain.FakeCardGenerator
import dev.notedeck.domain.FeedbackStore
import dev.notedeck.domain.HashingEmbedder
import dev.notedeck.domain.NoteIndex
import dev.notedeck.domain.OcrScanner
import dev.notedeck.domain.PlanStore
import dev.notedeck.domain.StatsSource
import dev.notedeck.domain.SummaryRouter
import kotlinx.coroutines.flow.first

@Module
@InstallIn(SingletonComponent::class)
object CardGeneratorModule {
    /** Every generator the screen can offer. The offline fake is for debug builds only. */
    @Provides
    fun cardGenerators(gemini: GeminiCardGenerator, claude: ClaudeCardGenerator): CardGenerators =
        CardGenerators(listOfNotNull(gemini, claude, FakeCardGenerator().takeIf { BuildConfig.DEBUG }))

    /** The phone first; the backend only when the learner turned on cloud AI, read fresh on every call. */
    @Provides
    fun summaryRouter(onDevice: MlKitSummarizer, cloud: CloudSummarizer, settings: SettingsRepository): SummaryRouter =
        SummaryRouter(onDevice, cloud) { settings.settings.first().allowCloudAi }

    @Provides
    fun ocrScanner(scanner: MlKitOcrScanner): OcrScanner = scanner

    /** A model-free embedder for now; a neural one can replace it here without touching search or the screens. */
    @Provides
    fun embedder(): Embedder = HashingEmbedder()

    @Provides
    fun noteDao(database: NoteDeckDatabase): NoteDao = database.noteDao()

    @Provides
    fun noteIndex(index: RoomNoteIndex): NoteIndex = index

    @Provides
    fun answerer(answerer: BackendAnswerer): Answerer = answerer

    @Provides
    fun coach(coach: BackendCoach): Coach = coach

    @Provides
    fun stats(source: RoomStatsSource): StatsSource = source

    @Provides
    fun planStore(store: SettingsPlanStore): PlanStore = store

    @Provides
    fun feedbackDao(database: NoteDeckDatabase): FeedbackDao = database.feedbackDao()

    @Provides
    fun feedbackStore(store: RoomFeedbackStore): FeedbackStore = store
}
