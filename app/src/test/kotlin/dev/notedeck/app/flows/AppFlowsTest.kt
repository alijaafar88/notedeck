package dev.notedeck.app.flows

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dev.notedeck.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Two whole flows through the real app: MainActivity, navigation and ViewModels, with the repository
 * replaced by TestDeckRepository (see RepositoryInjectionTest). Robolectric runs them on the JVM.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [35])
class AppFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    /** The repository's Flow is collected on Dispatchers.IO, which the rule doesn't track, so wait for the text. */
    private fun waitFor(text: String) {
        rule.waitUntil(timeoutMillis = 5_000) { rule.onAllNodesWithTextExists(text) }
        rule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextExists(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private fun pressBack() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun addingACardUpdatesTheDeck() {
        waitFor("Test deck")
        rule.onNodeWithText("Test deck").performClick()
        waitFor("0 cards")

        rule.onNodeWithText("Add card").performClick()
        rule.onNodeWithText("Front").performTextInput("val or var?")
        rule.onNodeWithText("Back").performTextInput("val")
        rule.onNodeWithText("Save").performClick()
        pressBack()

        waitFor("1 card")
        rule.onNodeWithText("val or var?").assertIsDisplayed()
    }

    @Test
    fun studyingADeckToTheEnd() {
        waitFor("Study deck")
        rule.onNodeWithText("Study deck").performClick()
        waitFor("Study this deck")
        rule.onNodeWithText("Study this deck").performClick()

        waitFor("Card 1 of 2")
        rule.onNodeWithText("Show answer").performClick()
        rule.onNodeWithText("Knew it").performClick()
        waitFor("Card 2 of 2")
        rule.onNodeWithText("Show answer").performClick()
        rule.onNodeWithText("Missed it").performClick()

        waitFor("Done: you knew 1 of 2")
        rule.onNodeWithText("Done").performClick()
        waitFor("Study this deck")
    }
}
