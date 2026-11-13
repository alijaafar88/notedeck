package dev.notedeck.backend

import dev.notedeck.domain.Grounding
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json

class CoachTest {
    private val snapshot = SnapshotDto(
        dailyGoal = 20,
        decks = listOf(DeckDto("Kotlin basics", 4, 2, 10, 3), DeckDto("Coroutines", 6, 5, 2, 0)),
        reviewsPerDay = listOf(0, 3, 5, 0, 8, 2, 4),
    )

    @Test
    fun `the tools only describe the snapshot`() {
        val tools = CoachTools(snapshot)
        assertEquals(
            "Kotlin basics: 4 cards, 2 due now, 10 reviews this week, 3 Again\nCoroutines: 6 cards, 5 due now, 2 reviews this week, 0 Again",
            tools.deckStats(),
        )
        assertEquals("0, 3, 5, 0, 8, 2, 4", tools.reviewsPerDay())
        assertEquals(20, tools.currentGoal())
    }

    @Test
    fun `a plan in a code fence parses, but chatty text doesn't`() {
        assertEquals(PlanDto(25, listOf("Coroutines"), "Most due"), parsePlan("```json\n{\"dailyGoal\": 25, \"focusDecks\": [\"Coroutines\"], \"reason\": \"Most due\"}\n```"))
        assertNull(parsePlan("Here's your plan: study more."))
    }

    @Test
    fun `the route returns the agent's plan as JSON`() = testApplication {
        val received = mutableListOf<SnapshotDto>()
        val coach = CoachAgent {
            received += it
            """{"dailyGoal": 25, "focusDecks": ["Coroutines"], "reason": "Five cards are due there."}"""
        }
        application { cardsModule({ _, _ -> flowOf() }, RateLimiter(perMinute = 10), coach) }
        val response = client.post("/coach") { setBody(Json.encodeToString(snapshot)) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(PlanDto(25, listOf("Coroutines"), "Five cards are due there."), Json.decodeFromString<PlanDto>(response.bodyAsText()))
        assertEquals(listOf(snapshot), received)
    }

    @Test
    fun `nonsense from the agent, or a failure, is a 502 the app can explain`() = testApplication {
        var reply: () -> String = { "I think you should study." }
        application { cardsModule({ _, _ -> flowOf() }, RateLimiter(perMinute = 10)) { reply() } }
        assertEquals(HttpStatusCode.BadGateway, client.post("/coach") { setBody(Json.encodeToString(snapshot)) }.status)
        reply = { error("Anthropic is down") }
        assertEquals(HttpStatusCode.BadGateway, client.post("/coach") { setBody(Json.encodeToString(snapshot)) }.status)
        assertEquals(HttpStatusCode.BadRequest, client.post("/coach") { setBody("not json") }.status)
    }

    @Test
    fun `the answer route numbers the sources for the model`() = testApplication {
        val prompts = mutableListOf<Pair<String, String>>()
        application { cardsModule({ system, user -> prompts += system to user; flowOf("Hot [1].") }, RateLimiter(perMinute = 10)) }
        val body = client.post("/answer") { setBody("""{"question": "Hot or cold?", "sources": ["Channels are hot", "Flows are cold"]}""") }.bodyAsText()
        assertEquals("data: \"Hot [1].\"\n\nevent: done\ndata: \n\n", body)
        assertEquals(Grounding.SYSTEM_PROMPT to Grounding.userPrompt("Hot or cold?", listOf("Channels are hot", "Flows are cold")), prompts.single())
        assertEquals(HttpStatusCode.BadRequest, client.post("/answer") { setBody("""{"question": "Q", "sources": []}""") }.status)
    }
}
