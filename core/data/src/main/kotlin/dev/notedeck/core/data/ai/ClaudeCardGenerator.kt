package dev.notedeck.core.data.ai

import dev.notedeck.domain.CardGenerator
import dev.notedeck.domain.DraftEvent
import dev.notedeck.domain.NotesPhoto
import dev.notedeck.domain.Privacy
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Claude through NoteDeck's backend, which holds the Anthropic key and builds the prompt. */
class ClaudeCardGenerator @Inject constructor(private val backend: BackendClient) : CardGenerator {
    override val id = "claude"
    override val name = "Claude"
    override val acceptsPhotos = false
    override val available: Boolean get() = backend.configured

    override fun draft(notes: String, photo: NotesPhoto?): Flow<DraftEvent> = backend.stream("/cards", Privacy.redact(notes)).toDraftEvents()
}
