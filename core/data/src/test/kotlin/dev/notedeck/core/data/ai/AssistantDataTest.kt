package dev.notedeck.core.data.ai

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.notedeck.core.data.InMemoryPreferencesStore
import dev.notedeck.core.data.NoteDeckDatabase
import dev.notedeck.core.data.RoomDeckRepository
import dev.notedeck.core.data.SettingsRepository
import dev.notedeck.domain.AiRating
import dev.notedeck.domain.DeckStats
import dev.notedeck.domain.FeedbackCounts
import dev.notedeck.domain.HashingEmbedder
import dev.notedeck.domain.WeeklyPlan
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import dev.notedeck.model.Review
import dev.notedeck.model.ReviewGrade
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AssistantDataTest {
    private lateinit var db: NoteDeckDatabase
    private val now = Instant.parse("2026-11-13T09:00:00Z")
    private val clock = object : Clock {
        override fun now() = now
    }

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<NoteDeckDatabase>(RuntimeEnvironment.getApplication()).setDriver(AndroidSQLiteDriver()).build()
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun `vectors survive the round trip through a BLOB`() {
        val vector = floatArrayOf(0.5f, -1f, 2f)
        assertEquals(vector.toList(), vector.toBytes().toVector().toList())
        assertEquals(12, vector.toBytes().size)
    }

    @Test
    fun `saved notes are chunked, embedded and found by meaning-adjacent words`() = runTest {
        val index = RoomNoteIndex(db.noteDao(), HashingEmbedder(), clock, StandardTestDispatcher(testScheduler))
        index.add("Coroutines", "Channel: a hot pipe between coroutines\nJob: a handle to a coroutine\n\nRoom stores data in SQLite")
        val passages = index.search("What are channels?", k = 2)
        assertEquals("Coroutines", passages.first().deckName)
        assertEquals("Channel: a hot pipe between coroutines\nJob: a handle to a coroutine", passages.first().text)
        assertEquals(2, db.noteDao().chunks().size)
        assertEquals(1, db.noteDao().noteCount())
    }

    @Test
    fun `the coach's snapshot counts cards, due cards and this week's reviews per deck`() = runTest {
        val decks = RoomDeckRepository(db.deckDao())
        decks.decks.first() // seeds the sample decks
        db.clearAllTables()
        decks.addDeck(Deck("Coroutines", listOf(Card("c1", "Q1", "A1", box = 1), Card("c2", "Q2", "A2", box = 3))))
        decks.recordReview(Review("c1", ReviewGrade.AGAIN, now - 2.hours))   // box 1: due again after a day, so not due yet
        decks.recordReview(Review("c2", ReviewGrade.GOOD, now - 10.days))    // box 3: due after 4 days, so due; and outside this week
        val settings = SettingsRepository(InMemoryPreferencesStore()).apply { setDailyGoal(30) }
        val snapshot = RoomStatsSource(db.deckDao(), settings, clock, StandardTestDispatcher(testScheduler)).snapshot()
        assertEquals(30, snapshot.dailyGoal)
        assertEquals(listOf(DeckStats("Coroutines", cards = 2, dueNow = 1, reviewsThisWeek = 1, againThisWeek = 1)), snapshot.decks)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 1), snapshot.reviewsPerDay)
    }

    @Test
    fun `ratings are counted by kind, and stay in the local database`() = runTest {
        val store = RoomFeedbackStore(db.feedbackDao(), clock)
        store.record("generate", "cards-v2", AiRating.Helpful)
        store.record("ask", "answer-v1", AiRating.Helpful)
        store.record("generate", "cards-v2", AiRating.Reported)
        assertEquals(FeedbackCounts(helpful = 2, notHelpful = 0, reported = 1), store.counts.first())
    }

    @Test
    fun `applying a plan sets the goal and remembers the focus`() = runTest {
        val settings = SettingsRepository(InMemoryPreferencesStore())
        val store = SettingsPlanStore(settings)
        assertEquals(null, store.active.first())
        store.apply(WeeklyPlan(25, listOf("Coroutines", "Kotlin basics"), "Most due cards"))
        assertEquals(WeeklyPlan(25, listOf("Coroutines", "Kotlin basics"), "Most due cards"), store.active.first())
        assertEquals(25, settings.settings.first().dailyGoal)
    }
}
