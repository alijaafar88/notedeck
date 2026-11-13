package dev.notedeck.core.data.ai

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer as MlKitClient
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.notedeck.domain.NotesPhoto
import dev.notedeck.domain.OcrScanner
import dev.notedeck.domain.Summarizer
import dev.notedeck.domain.SummarizerStatus
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await

/** Gemini Nano through ML Kit GenAI summarization (beta). Phones without AICore, such as the emulator, report Unavailable. */
class MlKitSummarizer @Inject constructor(@ApplicationContext private val context: Context) : Summarizer {
    override val onDevice = true

    private fun client() = Summarization.getClient(
        SummarizerOptions.builder(context)
            .setInputType(SummarizerOptions.InputType.ARTICLE)
            .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
            .setLanguage(SummarizerOptions.Language.ENGLISH)
            .build(),
    )

    /** Each call gets its own client, closed only after its future has completed. */
    private suspend fun <T> using(block: suspend (MlKitClient) -> T): T {
        val client = client()
        return try {
            block(client)
        } finally {
            client.close()
        }
    }

    override suspend fun status(): SummarizerStatus = try {
        using { statusOf(it.checkFeatureStatus().await()) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // No AICore, or an unsupported phone: treat both as "not here".
        SummarizerStatus.Unavailable
    }

    override suspend fun download() {
        using {
            it.downloadFeature(object : DownloadCallback {
                override fun onDownloadStarted(bytesToDownload: Long) {}
                override fun onDownloadProgress(totalBytesDownloaded: Long) {}
                override fun onDownloadCompleted() {}
                override fun onDownloadFailed(e: GenAiException) {}
            }).await()
        }
    }

    override suspend fun summarize(text: String): String =
        using { it.runInference(SummarizationRequest.builder(text).build()).await().summary }

    companion object {
        fun statusOf(featureStatus: Int): SummarizerStatus = when (featureStatus) {
            FeatureStatus.AVAILABLE -> SummarizerStatus.Available
            FeatureStatus.DOWNLOADABLE -> SummarizerStatus.Downloadable
            FeatureStatus.DOWNLOADING -> SummarizerStatus.Downloading
            else -> SummarizerStatus.Unavailable
        }
    }
}

/** Summaries from NoteDeck's backend (Claude). Used only when the learner allowed cloud AI. */
class CloudSummarizer @Inject constructor(private val backend: BackendClient) : Summarizer {
    override val onDevice = false

    override suspend fun status() = if (backend.configured) SummarizerStatus.Available else SummarizerStatus.Unavailable

    override suspend fun summarize(text: String): String = backend.stream("/summary", text).toList().joinToString("").trim()
}

/** ML Kit text recognition with the model bundled in the APK: works offline, the first time. */
class MlKitOcrScanner @Inject constructor() : OcrScanner {
    override suspend fun read(photo: NotesPhoto): String? {
        val bitmap = BitmapFactory.decodeByteArray(photo.jpeg, 0, photo.jpeg.size) ?: return null
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
            // Blocks are paragraphs; joining them with line breaks keeps "term: definition" lines intact.
            return result.textBlocks.joinToString("\n") { it.text }.trim().ifEmpty { null }
        } finally {
            recognizer.close()
        }
    }
}
