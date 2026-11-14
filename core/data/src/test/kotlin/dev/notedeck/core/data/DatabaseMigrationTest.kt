package dev.notedeck.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.notedeck.domain.AiRating
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Builds a real version 1 database from the committed schema, then lets Room upgrade it. Learners must keep their decks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {
    private fun createVersion1(file: File) {
        val schemas = File(requireNotNull(System.getProperty("schemas.dir")) { "schemas.dir not set" })
        val schema = JSONObject(schemas.resolve("dev.notedeck.core.data.NoteDeckDatabase/1.json").readText()).getJSONObject("database")
        file.parentFile?.mkdirs()
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices")
                for (j in 0 until (indices?.length() ?: 0)) {
                    db.execSQL(indices!!.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO decks (id, name) VALUES (1, 'Kotlin basics')")
            db.execSQL("INSERT INTO cards (id, deckId, front, back, box, position) VALUES ('1', 1, 'val or var?', 'val', 2, 0)")
            db.version = 1
        }
    }

    @Test
    fun `upgrading from version 1 keeps decks and adds the shared-deck cache, the notes tables and feedback`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val file = context.getDatabasePath("migration-test.db")
        createVersion1(file)

        val database = Room.databaseBuilder<NoteDeckDatabase>(context, file.absolutePath)
            .setDriver(AndroidSQLiteDriver())
            .build()
        try {
            val decks = database.deckDao().observeDecksWithCards().first()
            assertEquals(listOf("Kotlin basics"), decks.map { it.deck.name })
            assertEquals(2, decks.single().cards.single().box)

            val shared = database.sharedDeckDao()
            shared.upsertAll(listOf(SharedDeckEntity("a", "A", 3, Instant.fromEpochMilliseconds(5))))
            assertEquals(Instant.fromEpochMilliseconds(5), shared.lastFetched())

            // Version 3's notes tables exist and work.
            val notes = database.noteDao()
            notes.insert(NoteEntity(deckName = "Kotlin basics", text = "val is read-only", createdAt = Instant.fromEpochMilliseconds(6)), listOf(ChunkEntity(noteId = 0, position = 0, text = "val is read-only", embedding = byteArrayOf(1, 2, 3, 4))))
            assertEquals(listOf("val is read-only"), notes.chunks().map { it.text })

            // Version 4's feedback table.
            database.feedbackDao().insert(FeedbackEntity(feature = "ask", promptVersion = "answer-v1", rating = AiRating.Helpful, createdAt = Instant.fromEpochMilliseconds(7)))
            assertEquals(listOf(RatingCount(AiRating.Helpful, 1)), database.feedbackDao().observeCounts().first())
        } finally {
            database.close()
        }
    }
}
