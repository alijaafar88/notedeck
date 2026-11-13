package dev.notedeck.core.data.ai

import dev.notedeck.core.data.ChunkEntity
import dev.notedeck.core.data.DeckDao
import dev.notedeck.core.data.NoteDao
import dev.notedeck.core.data.NoteEntity
import dev.notedeck.core.data.SettingsRepository
import dev.notedeck.domain.Answerer
import dev.notedeck.domain.Chunker
import dev.notedeck.domain.Coach
import dev.notedeck.domain.DeckStats
import dev.notedeck.domain.Embedder
import dev.notedeck.domain.IoDispatcher
import dev.notedeck.domain.NoteIndex
import dev.notedeck.domain.Passage
import dev.notedeck.domain.PlanStore
import dev.notedeck.domain.StatsSource
import dev.notedeck.domain.StudySnapshot
import dev.notedeck.domain.WeeklyPlan
import dev.notedeck.domain.cosine
import dev.notedeck.domain.isDue
import dev.notedeck.model.ReviewGrade
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun FloatArray.toBytes(): ByteArray = ByteBuffer.allocate(size * Float.SIZE_BYTES).also { buffer -> forEach { buffer.putFloat(it) } }.array()

fun ByteArray.toVector(): FloatArray = ByteBuffer.wrap(this).let { buffer -> FloatArray(size / Float.SIZE_BYTES) { buffer.float } }

/** Notes are chunked and embedded once, when saved; a search embeds only the question. Everything stays on the phone. */
class RoomNoteIndex @Inject constructor(
    private val dao: NoteDao,
    private val embedder: Embedder,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : NoteIndex {
    override suspend fun add(deckName: String, text: String) = withContext(io) {
        val chunks = Chunker.chunks(text)
        if (chunks.isEmpty()) return@withContext
        dao.insert(
            NoteEntity(deckName = deckName, text = text, createdAt = clock.now()),
            chunks.mapIndexed { i, chunk -> ChunkEntity(noteId = 0, position = i, text = chunk, embedding = embedder.embed(chunk).toBytes()) },
        )
    }

    override suspend fun search(query: String, k: Int): List<Passage> = withContext(io) {
        val q = embedder.embed(query)
        dao.chunks()
            .map { Passage(it.noteId, it.deckName, it.text, cosine(q, it.embedding.toVector())) }
            .sortedByDescending { it.score }
            .take(k)
    }
}

/** Answers through the backend's /answer route: numbered sources in, a cited answer streamed out. */
class BackendAnswerer @Inject constructor(private val backend: BackendClient) : Answerer {
    @Serializable
    private data class Request(val question: String, val sources: List<String>)

    override fun answer(question: String, sources: List<String>): Flow<String> =
        backend.stream("/answer", Json.encodeToString(Request(question, sources)), BackendClient.JSON)
}

/** The backend's study coach: a snapshot of statistics in, a proposed plan out. Validation happens in the ViewModel. */
class BackendCoach @Inject constructor(private val backend: BackendClient) : Coach {
    @Serializable
    data class DeckDto(val name: String, val cards: Int, val dueNow: Int, val reviewsThisWeek: Int, val againThisWeek: Int)

    @Serializable
    data class SnapshotDto(val dailyGoal: Int, val decks: List<DeckDto>, val reviewsPerDay: List<Int>)

    @Serializable
    data class PlanDto(val dailyGoal: Int, val focusDecks: List<String>, val reason: String)

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun propose(snapshot: StudySnapshot): WeeklyPlan {
        val body = SnapshotDto(
            snapshot.dailyGoal,
            snapshot.decks.map { DeckDto(it.name, it.cards, it.dueNow, it.reviewsThisWeek, it.againThisWeek) },
            snapshot.reviewsPerDay,
        )
        val plan = json.decodeFromString<PlanDto>(backend.post("/coach", json.encodeToString(body), BackendClient.JSON))
        return WeeklyPlan(plan.dailyGoal, plan.focusDecks, plan.reason)
    }
}

/** The coach's snapshot, computed from Room and settings. Read-only. */
class RoomStatsSource @Inject constructor(
    private val decks: DeckDao,
    private val settings: SettingsRepository,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StatsSource {
    override suspend fun snapshot(): StudySnapshot = withContext(io) {
        val now = clock.now()
        val weekAgo = now - 7.days
        val schedules = decks.deckSchedules().groupBy { it.deckName }
        val reviews = decks.reviewsSince(weekAgo)
        val byDeck = reviews.groupBy { it.deckName }
        val deckStats = schedules.map { (name, cards) ->
            DeckStats(
                name = name,
                cards = cards.size,
                dueNow = cards.count { isDue(it.box, it.lastReviewedAt, now) },
                reviewsThisWeek = byDeck[name].orEmpty().size,
                againThisWeek = byDeck[name].orEmpty().count { it.grade == ReviewGrade.AGAIN },
            )
        }.sortedBy { it.name }
        val perDay = (6 downTo 0).map { daysAgo ->
            val start = now - (daysAgo + 1).days
            val end = now - daysAgo.days
            reviews.count { it.reviewedAt > start && it.reviewedAt <= end }
        }
        StudySnapshot(settings.currentSettings().dailyGoal, deckStats, perDay)
    }
}

/** An applied plan: the daily goal changes, and the focus decks and the reason are remembered. */
class SettingsPlanStore @Inject constructor(private val settings: SettingsRepository) : PlanStore {
    override val active: Flow<WeeklyPlan?> = settings.settings.map { s ->
        if (s.focusDecks.isEmpty()) null else WeeklyPlan(s.dailyGoal, s.focusDecks, s.planReason)
    }

    override suspend fun apply(plan: WeeklyPlan) = settings.applyPlan(plan.dailyGoal, plan.focusDecks, plan.reason)
}
