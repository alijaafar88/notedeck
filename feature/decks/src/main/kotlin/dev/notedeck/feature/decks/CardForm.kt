package dev.notedeck.feature.decks

fun canSave(front: String, back: String): Boolean = front.isNotBlank() && back.isNotBlank()
