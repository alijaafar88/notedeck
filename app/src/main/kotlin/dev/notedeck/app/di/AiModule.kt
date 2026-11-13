package dev.notedeck.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.notedeck.domain.FakeTextModel
import dev.notedeck.domain.TextModel

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    /** The fake for now. AI3 and AI4 bind real models here; nothing that uses TextModel has to change. */
    @Provides
    fun textModel(): TextModel = FakeTextModel()
}
