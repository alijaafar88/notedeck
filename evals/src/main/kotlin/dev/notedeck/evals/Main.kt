package dev.notedeck.evals

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import dev.notedeck.domain.CardRules
import java.io.File
import java.time.LocalDate
import kotlin.system.exitProcess

/**
 *   ./gradlew :evals:run                            the stored reference replies (CI, every push)
 *   ./gradlew :evals:run --args=live                the real model, with ANTHROPIC_API_KEY in the environment
 *   ./gradlew :evals:run --args="live --record"     ...and save its replies as the new reference set
 * Exits with an error when the report misses the gate, so CI fails.
 */
fun main(args: Array<String>) {
    val golden = Evals.loadGolden()
    val live = "live" in args
    val model = System.getenv("CLAUDE_MODEL") ?: "claude-opus-5-5"
    val replies = if (live) generate(golden, model) else Evals.loadReplies().replies
    val report = Evals.run(golden, replies, source = if (live) "$model, live" else "the stored reference set")
    println(report.format())
    if (live && "--record" in args) {
        val about = "Recorded from $model on ${LocalDate.now()} with prompt ${CardRules.PROMPT_VERSION}. Re-record after changing the prompt or the model."
        File("data/reference-replies.json").writeText(Evals.json.encodeToString(ReplySet(about, CardRules.PROMPT_VERSION, replies)) + "\n")
        println("Recorded ${replies.size} replies to data/reference-replies.json")
    }
    if (!report.ok) exitProcess(1)
}

/** One request per golden note, with exactly the prompt the backend uses. The key comes from the environment. */
private fun generate(golden: GoldenSet, model: String): Map<String, String> {
    val client = AnthropicOkHttpClient.fromEnv()
    return golden.cases.associate { case ->
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(16_000L)
            .system(CardRules.SYSTEM_PROMPT)
            .addUserMessage(CardRules.userPrompt(case.notes))
            .build()
        val reply = client.messages().create(params).content().mapNotNull { block -> block.text().orElse(null)?.text() }.joinToString("")
        case.id to reply
    }
}
