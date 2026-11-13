package dev.notedeck.app.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object DecksKey : NavKey
@Serializable data class DeckKey(val name: String) : NavKey
@Serializable data class StudyKey(val deckName: String? = null) : NavKey
@Serializable data class AddCardKey(val deckName: String) : NavKey
@Serializable data object SharedDecksKey : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data object PlaygroundKey : NavKey
@Serializable data class GenerateKey(val deckName: String) : NavKey
