package dev.notedeck.core.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import app.cash.turbine.test
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import dev.notedeck.model.Review
import dev.notedeck.model.ReviewGrade
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomDeckRepositoryTest {
    private lateinit var db: NoteDeckDatabase
    private lateinit var repository: RoomDeckRepository

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<NoteDeckDatabase>(RuntimeEnvironment.getApplication())
            .setDriver(AndroidSQLiteDriver())
            .build()
        repository = RoomDeckRepository(db.deckDao())
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun `a new install is seeded with the sample decks, once`() = runTest {
        repository.decks.first()
        val decks = repository.decks.first()
        assertEquals(SampleDecks.all.map { it.name }.sorted(), decks.map { it.name })
        assertEquals(SampleDecks.all.sumOf { it.cards.size }, decks.sumOf { it.cards.size })
    }

    @Test
    fun `an added card is read back as a domain card, at the end of its deck`() = runTest {
        repository.decks.test {
            awaitItem()
            repository.addCard("Kotlin basics", Card("new", "What does ?: do?", "Supplies a default for null"))
            val deck = awaitItem().first { it.name == "Kotlin basics" }
            assertEquals(Card("new", "What does ?: do?", "Supplies a default for null"), deck.cards.last())
        }
    }

    @Test
    fun `a card for a missing deck is ignored`() = runTest {
        repository.decks.first()
        repository.addCard("No such deck", Card("x", "Q", "A"))
        assertEquals(0, repository.decks.first().sumOf { deck -> deck.cards.count { it.id == "x" } })
    }

    @Test
    fun `addDeck stores the deck and its cards`() = runTest {
        repository.decks.first()
        repository.addDeck(Deck("Flow", listOf(Card("f1", "Cold or hot?", "Cold"))))
        assertEquals(listOf("f1"), repository.decks.first().first { it.name == "Flow" }.cards.map { it.id })
    }

    @Test
    fun `a recorded review is stored, with its grade and time`() = runTest {
        val card = repository.decks.first().first().cards.first()
        val review = Review(card.id, ReviewGrade.GOOD, Instant.parse("2026-11-12T09:00:00Z"))
        repository.recordReview(review)
        assertEquals(listOf(review), db.deckDao().observeReviews().first().map { it.toDomain() })
    }
}
