package dev.notedeck.domain

import dev.notedeck.domain.SummarizerStatus.Available
import dev.notedeck.domain.SummarizerStatus.Downloadable
import dev.notedeck.domain.SummarizerStatus.Downloading
import dev.notedeck.domain.SummarizerStatus.Unavailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class SummaryRouterTest {
    private class Fake(override val onDevice: Boolean, private val status: SummarizerStatus, private val fails: Boolean = false) : Summarizer {
        val calls = mutableListOf<String>()

        override suspend fun status() = status

        override suspend fun summarize(text: String): String {
            calls += text
            if (fails) throw IllegalStateException("busy")
            return if (onDevice) "phone" else "cloud"
        }
    }

    private data class Case(val phone: SummarizerStatus, val cloud: SummarizerStatus, val allowed: Boolean, val route: SummaryRoute)

    @Test
    fun `the route for every combination`() = runTest {
        listOf(
            Case(Available, Available, allowed = true, SummaryRoute.OnDevice),
            Case(Available, Unavailable, allowed = false, SummaryRoute.OnDevice),
            Case(Unavailable, Available, allowed = true, SummaryRoute.Cloud),
            Case(Unavailable, Available, allowed = false, SummaryRoute.NotOnThisPhone),
            Case(Unavailable, Unavailable, allowed = true, SummaryRoute.NotOnThisPhone),
            Case(Downloadable, Available, allowed = false, SummaryRoute.NeedsDownload),
            Case(Downloadable, Available, allowed = true, SummaryRoute.Cloud),
            Case(Downloading, Unavailable, allowed = true, SummaryRoute.Downloading),
        ).forEach { case ->
            val router = SummaryRouter(Fake(true, case.phone), Fake(false, case.cloud)) { case.allowed }
            assertEquals(case.route, router.route(), "$case")
        }
    }

    @Test
    fun `the phone summarises when it can, and the cloud is never called`() = runTest {
        val cloud = Fake(false, Available)
        val summary = SummaryRouter(Fake(true, Available), cloud) { true }.summarize("notes")
        assertEquals(Summary("phone", onDevice = true), summary)
        assertEquals(emptyList(), cloud.calls)
    }

    @Test
    fun `a failing phone falls back to the cloud only when allowed`() = runTest {
        assertEquals(
            Summary("cloud", onDevice = false),
            SummaryRouter(Fake(true, Available, fails = true), Fake(false, Available)) { true }.summarize("notes"),
        )
        val cloud = Fake(false, Available)
        assertFailsWith<IllegalStateException> { SummaryRouter(Fake(true, Available, fails = true), cloud) { false }.summarize("notes") }
        assertEquals(emptyList(), cloud.calls, "nothing is uploaded without permission")
    }

    @Test
    fun `with no route, summarising explains why`() = runTest {
        assertFailsWith<NoSummarizerException> { SummaryRouter(Fake(true, Unavailable), Fake(false, Available)) { false }.summarize("notes") }
    }
}
