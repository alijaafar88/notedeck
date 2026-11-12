package dev.notedeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.notedeck.app.ui.DeckListScreen
import dev.notedeck.app.ui.NoteDeckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteDeckTheme {
                DeckListScreen(SampleDecks.all, onOpen = {})
            }
        }
    }
}
