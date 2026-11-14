package dev.notedeck.core.data

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    /** Re-emits after every write to decks or cards. */
    @Transaction
    @Query("SELECT * FROM decks ORDER BY name")
    fun observeDecksWithCards(): Flow<List<DeckWithCards>>

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun deckCount(): Int

    @Query("SELECT id FROM decks WHERE name = :name")
    suspend fun deckId(name: String): Long?

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    suspend fun cardCount(deckId: Long): Int

    /** Returns the new row id, or -1 if a deck with that name already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Upsert
    suspend fun upsertCards(cards: List<CardEntity>)

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteDeck(id: Long)

    @Insert
    suspend fun insertReview(review: ReviewEntity)

    @Query("SELECT * FROM reviews ORDER BY reviewedAt")
    fun observeReviews(): Flow<List<ReviewEntity>>

    /** Each card's box and when it was last reviewed (null if never): all the reminder needs. */
    @Query(
        "SELECT cards.box AS box, MAX(reviews.reviewedAt) AS lastReviewedAt FROM cards " +
            "LEFT JOIN reviews ON reviews.cardId = cards.id GROUP BY cards.id",
    )
    suspend fun schedules(): List<CardSchedule>

    /** schedules(), with each card's deck: for the coach's due counts. */
    @Query(
        "SELECT decks.name AS deckName, cards.box AS box, MAX(reviews.reviewedAt) AS lastReviewedAt FROM cards " +
            "JOIN decks ON decks.id = cards.deckId LEFT JOIN reviews ON reviews.cardId = cards.id GROUP BY cards.id",
    )
    suspend fun deckSchedules(): List<DeckCardSchedule>

    @Query(
        "SELECT decks.name AS deckName, reviews.grade AS grade, reviews.reviewedAt AS reviewedAt FROM reviews " +
            "JOIN cards ON cards.id = reviews.cardId JOIN decks ON decks.id = cards.deckId WHERE reviews.reviewedAt >= :since",
    )
    suspend fun reviewsSince(since: Instant): List<DeckReview>

    /** A deck and its cards, all or nothing. Returns false, and writes nothing, if the name is taken. */
    @Transaction
    suspend fun insertDeckWithCards(deck: DeckEntity, cards: List<CardEntity>): Boolean {
        val id = insertDeck(deck)
        if (id == -1L) return false
        upsertCards(cards.mapIndexed { index, card -> card.copy(deckId = id, position = index) })
        return true
    }
}

/** The shared-deck catalog: a cache of decks/index.json, refreshed from the network, read by the UI. */
@Dao
interface SharedDeckDao {
    @Query("SELECT * FROM shared_decks ORDER BY name")
    fun observeAll(): Flow<List<SharedDeckEntity>>

    @Query("SELECT MAX(fetchedAt) FROM shared_decks")
    suspend fun lastFetched(): Instant?

    @Query("DELETE FROM shared_decks")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertAll(decks: List<SharedDeckEntity>)

    /** The new catalog replaces the old one in one transaction, so readers never see it half-written. */
    @Transaction
    suspend fun replaceAll(decks: List<SharedDeckEntity>) {
        deleteAll()
        upsertAll(decks)
    }
}

/** Notes and their embedded chunks, for Ask. Search loads every chunk: fine for one learner's notes. */
@Dao
interface NoteDao {
    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Insert
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    /** A note and its chunks, all or nothing. The chunks' noteId is filled in here. */
    @Transaction
    suspend fun insert(note: NoteEntity, chunks: List<ChunkEntity>) {
        val id = insertNote(note)
        insertChunks(chunks.map { ChunkEntity(noteId = id, position = it.position, text = it.text, embedding = it.embedding) })
    }

    @Query(
        "SELECT note_chunks.noteId AS noteId, notes.deckName AS deckName, note_chunks.text AS text, note_chunks.embedding AS embedding " +
            "FROM note_chunks JOIN notes ON notes.id = note_chunks.noteId",
    )
    suspend fun chunks(): List<ChunkRow>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun noteCount(): Int
}

@Dao
interface FeedbackDao {
    @Insert
    suspend fun insert(feedback: FeedbackEntity)

    @Query("SELECT rating, COUNT(*) AS count FROM ai_feedback GROUP BY rating")
    fun observeCounts(): Flow<List<RatingCount>>
}

/**
 * Version 2 adds shared_decks; version 3 adds notes and their chunks; version 4 adds AI feedback. Room writes both migrations from the
 * exported schemas, so learners keep their decks.
 */
@Database(
    entities = [
        DeckEntity::class, CardEntity::class, ReviewEntity::class, SharedDeckEntity::class,
        NoteEntity::class, ChunkEntity::class, FeedbackEntity::class,
    ],
    version = 4,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
@ColumnTypeConverters(Converters::class)
abstract class NoteDeckDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao

    abstract fun sharedDeckDao(): SharedDeckDao

    abstract fun noteDao(): NoteDao

    abstract fun feedbackDao(): FeedbackDao
}
