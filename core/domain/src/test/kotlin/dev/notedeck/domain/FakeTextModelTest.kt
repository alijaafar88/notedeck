package dev.notedeck.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class FakeTextModelTest {
    @Test
    fun `at temperature 0 the reply is the same every time`() = runTest {
        val model = FakeTextModel()
        val request = TextRequest("What is a Flow", temperature = 0f)
        val first = model.generate(request).toList().joinToString("")
        assertEquals(first, model.generate(request).toList().joinToString(""))
        assertTrue(first.startsWith(FakeTextModel.OPENINGS.first()))
    }

    @Test
    fun `above 0 the opening varies`() = runTest {
        val model = FakeTextModel()
        val openings = (1..20).map { model.generate(TextRequest("Q", temperature = 1f)).toList().first() }.toSet()
        assertTrue(openings.size > 1, "$openings")
    }

    @Test
    fun `the reply streams a word at a time`() = runTest {
        val pieces = FakeTextModel().generate(TextRequest("Q")).toList()
        assertTrue(pieces.size > 5)
        assertTrue(pieces.drop(1).all { it.startsWith(" ") && ' ' !in it.drop(1) })
    }
}
