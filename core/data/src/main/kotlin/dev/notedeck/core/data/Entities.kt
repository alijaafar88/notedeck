package dev.notedeck.core.data

import androidx.room3.ColumnTypeConverter
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation
import dev.notedeck.model.Card
import dev.notedeck.model.Deck
import dev.notedeck.model.Review
import dev.notedeck.model.ReviewGrade
import kotlin.time.Instant

/** Deck names are unique: navigation and the import in D2 find decks by name. */
@Entity(tableName = "decks", indices = [Index(value = ["name"], unique = true)])
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(entity = DeckEntity::class, parentColumns = ["id"], childColumns = ["deckId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("deckId")],
)
data class CardEntity(
    @PrimaryKey val id: String,
    val deckId: Long,
    val front: String,
    val back: String,
    val box: Int = 1,
    /** Where the card sits in its deck. A @Relation has no ORDER BY, so the mapper sorts by this. */
    val position: Int = 0,
)

@Entity(
    tableName = "reviews",
    foreignKeys = [ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cardId")],
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    /** An enum: Room stores its name, so no converter is needed. */
    val grade: ReviewGrade,
    val reviewedAt: Instant,
)

/** One row of the cached shared-deck catalog, stamped with when it was fetched. */
@Entity(tableName = "shared_decks")
data class SharedDeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cardCount: Int,
    val fetchedAt: Instant,
)

/** The notes a learner generated cards from, kept so Ask can answer from them. */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckName: String,
    val text: String,
    val createdAt: Instant,
)

/** A passage of a note and its embedding: Float values, 4 bytes each, stored as a BLOB. */
@Entity(
    tableName = "note_chunks",
    foreignKeys = [ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId")],
)
class ChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val position: Int,
    val text: String,
    val embedding: ByteArray,
)

/** Not a table: a chunk with its note's deck, for search. */
data class ChunkRow(val noteId: Long, val deckName: String, val text: String, val embedding: ByteArray)

/** Not a table: a card's deck, box and last review, for the coach's statistics. */
data class DeckCardSchedule(val deckName: String, val box: Int, val lastReviewedAt: Instant?)

/** Not a table: one review with its deck, for the coach's statistics. */
data class DeckReview(val deckName: String, val grade: ReviewGrade, val reviewedAt: Instant)

/** Not a table: one row of DeckDao.schedules. */
data class CardSchedule(val box: Int, val lastReviewedAt: Instant?)

/** Not a table: a deck and its cards, loaded together. */
data class DeckWithCards(
    @Embedded val deck: DeckEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["deckId"])
    val cards: List<CardEntity>,
)

/** SQLite has no date type; instants are stored as epoch milliseconds, which sort correctly. */
class Converters {
    @ColumnTypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilliseconds()

    @ColumnTypeConverter
    fun toInstant(value: Long): Instant = Instant.fromEpochMilliseconds(value)
}

// Mappers: nothing above the data layer sees an entity.

fun CardEntity.toDomain() = Card(id = id, front = front, back = back, box = box)

fun Card.toEntity(deckId: Long, position: Int) = CardEntity(id = id, deckId = deckId, front = front, back = back, box = box, position = position)

fun DeckWithCards.toDomain() = Deck(name = deck.name, cards = cards.sortedBy { it.position }.map { it.toDomain() })

fun Review.toEntity() = ReviewEntity(cardId = cardId, grade = grade, reviewedAt = reviewedAt)

fun ReviewEntity.toDomain() = Review(cardId = cardId, grade = grade, reviewedAt = reviewedAt)
