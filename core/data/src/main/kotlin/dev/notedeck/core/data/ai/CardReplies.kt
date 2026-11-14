package dev.notedeck.core.data.ai

import dev.notedeck.domain.CardReplies
import dev.notedeck.domain.DraftEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Passes each text piece through as it arrives, then parses the whole reply once the stream completes. */
fun Flow<String>.toDraftEvents(): Flow<DraftEvent> = flow {
    val reply = StringBuilder()
    collect { piece ->
        reply.append(piece)
        emit(DraftEvent.Text(piece))
    }
    emit(DraftEvent.Done(CardReplies.parse(reply.toString())))
}
