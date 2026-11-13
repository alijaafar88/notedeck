package dev.notedeck.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records the code a real journey runs: start the app, open a deck. That code is compiled ahead of time on install. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startAndOpenADeck() = rule.collect(packageName = "dev.notedeck") {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Kotlin basics")), 10_000)
        device.findObject(By.text("Kotlin basics"))?.click()
        device.wait(Until.hasObject(By.text("Study this deck")), 10_000)
    }
}
