package dev.notedeck.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.notedeck.core.data.BuildConfig
import dev.notedeck.core.data.ai.ClaudeCardGenerator
import dev.notedeck.core.data.ai.GeminiCardGenerator
import dev.notedeck.domain.CardGenerators
import dev.notedeck.domain.FakeCardGenerator

@Module
@InstallIn(SingletonComponent::class)
object CardGeneratorModule {
    /** Every generator the screen can offer. The offline fake is for debug builds only. */
    @Provides
    fun cardGenerators(gemini: GeminiCardGenerator, claude: ClaudeCardGenerator): CardGenerators =
        CardGenerators(listOfNotNull(gemini, claude, FakeCardGenerator().takeIf { BuildConfig.DEBUG }))
}
