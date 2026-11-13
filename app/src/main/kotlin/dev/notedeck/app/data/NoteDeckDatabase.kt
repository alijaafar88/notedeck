package dev.notedeck.app.data

import androidx.room3.ColumnTypeConverters
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Upsert
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

    /** A deck and its cards, all or nothing. Returns false, and writes nothing, if the name is taken. */
    @Transaction
    suspend fun insertDeckWithCards(deck: DeckEntity, cards: List<CardEntity>): Boolean {
        val id = insertDeck(deck)
        if (id == -1L) return false
        upsertCards(cards.mapIndexed { index, card -> card.copy(deckId = id, position = index) })
        return true
    }
}

@Database(entities = [DeckEntity::class, CardEntity::class, ReviewEntity::class], version = 1)
@ColumnTypeConverters(Converters::class)
abstract class NoteDeckDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
}
