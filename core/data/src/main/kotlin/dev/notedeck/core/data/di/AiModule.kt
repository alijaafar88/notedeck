package dev.notedeck.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.notedeck.core.data.BuildConfig
import dev.notedeck.core.data.SettingsRepository
import dev.notedeck.core.data.ai.ClaudeCardGenerator
import dev.notedeck.core.data.ai.CloudSummarizer
import dev.notedeck.core.data.ai.GeminiCardGenerator
import dev.notedeck.core.data.ai.MlKitOcrScanner
import dev.notedeck.core.data.ai.MlKitSummarizer
import dev.notedeck.domain.CardGenerators
import dev.notedeck.domain.FakeCardGenerator
import dev.notedeck.domain.OcrScanner
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
}
