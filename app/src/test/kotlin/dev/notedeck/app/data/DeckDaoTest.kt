package dev.notedeck.app.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.notedeck.model.ReviewGrade
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Real SQL against an in-memory database: queries, keys, cascades and the converter, which a fake can't check. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeckDaoTest {
    private lateinit var db: NoteDeckDatabase
    private val dao get() = db.deckDao()

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<NoteDeckDatabase>(RuntimeEnvironment.getApplication())
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() = db.close()

    private fun card(id: String) = CardEntity(id = id, deckId = 0, front = "Q$id", back = "A$id")

    @Test
    fun `a deck comes back with its cards, in order`() = runTest {
        assertTrue(dao.insertDeckWithCards(DeckEntity(name = "Kotlin"), listOf(card("b"), card("a"))))
        val deck = dao.observeDecksWithCards().first().single()
        assertEquals("Kotlin", deck.deck.name)
        assertEquals(listOf("b", "a"), deck.toDomain().cards.map { it.id })
    }

    @Test
    fun `a second deck with the same name writes nothing`() = runTest {
        dao.insertDeckWithCards(DeckEntity(name = "Kotlin"), listOf(card("a")))
        assertFalse(dao.insertDeckWithCards(DeckEntity(name = "Kotlin"), listOf(card("b"))))
        val decks = dao.observeDecksWithCards().first()
        assertEquals(listOf(listOf("a")), decks.map { deck -> deck.cards.map { it.id } })
    }

    @Test
    fun `deleting a deck deletes its cards and their reviews`() = runTest {
        dao.insertDeckWithCards(DeckEntity(name = "Kotlin"), listOf(card("a")))
        dao.insertReview(ReviewEntity(cardId = "a", grade = ReviewGrade.GOOD, reviewedAt = Instant.fromEpochMilliseconds(1)))
        dao.deleteDeck(requireNotNull(dao.deckId("Kotlin")))
        assertEquals(0, dao.deckCount())
        assertEquals(emptyList(), dao.observeReviews().first())
    }

    @Test
    fun `reviews keep their grade and time`() = runTest {
        dao.insertDeckWithCards(DeckEntity(name = "Kotlin"), listOf(card("a")))
        val at = Instant.parse("2026-11-12T09:00:00Z")
        dao.insertReview(ReviewEntity(cardId = "a", grade = ReviewGrade.HARD, reviewedAt = at))
        val review = dao.observeReviews().first().single()
        assertEquals(ReviewGrade.HARD, review.grade)
        assertEquals(at, review.reviewedAt)
    }

    @Test
    fun `schedules give each card's box and latest review`() = runTest {
        dao.insertDeckWithCards(DeckEntity(name = "Kotlin"), listOf(card("a"), card("b")))
        dao.insertReview(ReviewEntity(cardId = "a", grade = ReviewGrade.GOOD, reviewedAt = Instant.fromEpochMilliseconds(10)))
        dao.insertReview(ReviewEntity(cardId = "a", grade = ReviewGrade.GOOD, reviewedAt = Instant.fromEpochMilliseconds(20)))
        val schedules = dao.schedules().sortedBy { it.lastReviewedAt?.toEpochMilliseconds() ?: -1 }
        assertEquals(listOf(CardSchedule(1, null), CardSchedule(1, Instant.fromEpochMilliseconds(20))), schedules)
    }
}
