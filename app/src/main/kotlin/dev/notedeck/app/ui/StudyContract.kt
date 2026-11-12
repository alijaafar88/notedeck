package dev.notedeck.app.ui

// M2: the Study screen's MVI contract. StudyViewModel's methods move into reduce, one intent at a time.

// TODO(m2): data class StudyState(deck, loading, position, known, revealed): the whole session in one value.

// TODO(m2): sealed interface StudyIntent: DeckLoaded(deck), Reveal, Answer(knewIt), Restart and Done.

// TODO(m2): sealed interface StudyEffect with Close, sent through a Channel when the user taps Done.

// TODO(m2): fun reduce(state: StudyState, intent: StudyIntent): StudyState, pure, with a table test.

// TODO(m2): fun StudyState.toUiState(): StudyUiState, so StudyScreen keeps drawing what it draws today.
