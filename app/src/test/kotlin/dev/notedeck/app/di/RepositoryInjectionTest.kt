package dev.notedeck.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dev.notedeck.domain.DeckRepository
import dev.notedeck.domain.FakeDeckRepository
import dev.notedeck.model.Deck
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A repository the test controls, with one known deck. */
class TestDeckRepository @Inject constructor() : DeckRepository by (FakeDeckRepository().apply { addDeck(Deck("Test deck", emptyList())) })

/** Replaces the app's RepositoryModule in every Hilt test in this source set. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
abstract class FakeRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDecks(fake: TestDeckRepository): DeckRepository
}

/** Most tests construct classes with fakes directly; this one checks the Hilt graph itself. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [35])
class RepositoryInjectionTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var repository: DeckRepository

    @Inject
    lateinit var sameRepository: DeckRepository

    @Before
    fun inject() = hilt.inject()

    @Test
    fun `the fake replaces the real repository`() = runTest {
        assertEquals(listOf("Test deck"), repository.decks.first().map { it.name })
    }

    @Test
    fun `the repository is a singleton`() {
        assertSame(repository, sameRepository)
    }
}
