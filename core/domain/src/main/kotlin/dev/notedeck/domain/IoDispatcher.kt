package dev.notedeck.domain

import javax.inject.Qualifier

/** The dispatcher for blocking work. A plain annotation, so features can ask for it without seeing the data layer. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
