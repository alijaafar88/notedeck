package dev.notedeck.core.data.ai

import com.google.mlkit.genai.common.FeatureStatus
import dev.notedeck.domain.SummarizerStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient

class CloudSummarizerTest {
    private val server = MockWebServer()

    @BeforeTest
    fun setUp() = server.start()

    @AfterTest
    fun tearDown() = server.close()

    @Test
    fun `the streamed pieces are joined into one summary, from the summary route`() = runTest {
        val body = listOf("- Flows are cold", "\n- collect starts them").joinToString("") { "data: ${Json.encodeToString(it)}\n\n" } + "event: done\ndata: \n\n"
        server.enqueue(MockResponse.Builder().addHeader("Content-Type", "text/event-stream").body(body).build())
        val summarizer = CloudSummarizer(BackendClient(OkHttpClient(), AiConfig(server.url("/").toString()), StandardTestDispatcher(testScheduler)))
        assertEquals("- Flows are cold\n- collect starts them", summarizer.summarize("notes"))
        assertEquals("/summary", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `without a backend the cloud summarizer is unavailable`() = runTest {
        val summarizer = CloudSummarizer(BackendClient(OkHttpClient(), AiConfig(null), StandardTestDispatcher(testScheduler)))
        assertEquals(SummarizerStatus.Unavailable, summarizer.status())
    }

    @Test
    fun `ML Kit's feature status maps onto NoteDeck's`() {
        assertEquals(
            listOf(SummarizerStatus.Available, SummarizerStatus.Downloadable, SummarizerStatus.Downloading, SummarizerStatus.Unavailable),
            listOf(FeatureStatus.AVAILABLE, FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING, FeatureStatus.UNAVAILABLE).map(MlKitSummarizer::statusOf),
        )
    }
}
