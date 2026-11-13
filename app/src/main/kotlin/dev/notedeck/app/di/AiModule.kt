package dev.notedeck.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.notedeck.app.BuildConfig
import dev.notedeck.core.data.ai.AiConfig
import dev.notedeck.domain.FakeTextModel
import dev.notedeck.domain.TextModel

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    /** The fake for now. AI3 and AI4 bind real models here; nothing that uses TextModel has to change. */
    @Provides
    fun textModel(): TextModel = FakeTextModel()

    /** The backend that holds the Anthropic key. Set notedeck.proxyUrl in local.properties; debug builds default to the emulator's host. */
    @Provides
    fun aiConfig(): AiConfig = AiConfig(proxyUrl = BuildConfig.PROXY_URL.ifBlank { null })
}
