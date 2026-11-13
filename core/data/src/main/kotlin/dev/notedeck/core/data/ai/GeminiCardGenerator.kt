package dev.notedeck.core.data.ai

import android.content.Context
import android.graphics.BitmapFactory
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.notedeck.domain.CardGenerator
import dev.notedeck.domain.CardRules
import dev.notedeck.domain.DraftEvent
import dev.notedeck.domain.NotesPhoto
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Gemini through Firebase AI Logic. The app holds no Gemini key: Firebase forwards the call, and App Check
 * (installed in release builds) proves it comes from this app. Unavailable until Firebase is initialised.
 */
class GeminiCardGenerator @Inject constructor(@ApplicationContext private val context: Context) : CardGenerator {
    override val id = "gemini"
    override val name = "Gemini"
    override val acceptsPhotos = true
    override val available: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = Schema.array(Schema.obj(mapOf("front" to Schema.string(), "back" to Schema.string())))
            },
            systemInstruction = content { text(CardRules.SYSTEM_PROMPT) },
        )
    }

    override fun draft(notes: String, photo: NotesPhoto?): Flow<DraftEvent> {
        if (!available) return flow { throw IllegalStateException("Gemini isn't set up. Add the Firebase values to local.properties.") }
        val request = content {
            photo?.let { BitmapFactory.decodeByteArray(it.jpeg, 0, it.jpeg.size) }?.let { image(it) }
            text(CardRules.userPrompt(notes.ifBlank { "(The notes are in the photo.)" }))
        }
        return model.generateContentStream(request).map { it.text.orEmpty() }.toDraftEvents()
    }

    companion object {
        /** Model names change; this is the one place to update. */
        const val MODEL = "gemini-3.8-flash"
    }
}
