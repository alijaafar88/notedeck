package dev.notedeck.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// TODO(c3): a stateless AddCardScreen with front, back and callbacks, and an AddCardRoute that owns the state
// with rememberSaveable and shows a snackbar on save.
@Composable
fun AddCardRoute(onSaved: (front: String, back: String) -> Unit, modifier: Modifier = Modifier) {
}
