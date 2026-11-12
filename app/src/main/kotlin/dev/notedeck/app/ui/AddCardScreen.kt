package dev.notedeck.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AddCardRoute(onSaved: (front: String, back: String) -> Unit, modifier: Modifier = Modifier) {
    var front by rememberSaveable { mutableStateOf("") }
    var back by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AddCardScreen(
            front = front,
            back = back,
            onFrontChange = { front = it },
            onBackChange = { back = it },
            onSave = {
                onSaved(front, back)
                front = ""
                back = ""
                scope.launch { snackbar.showSnackbar("Card saved") }
            },
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun AddCardScreen(
    front: String,
    back: String,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(front, onFrontChange, label = { Text("Front") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(back, onBackChange, label = { Text("Back") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onSave, enabled = canSave(front, back)) { Text("Save") }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddCardPreview() {
    NoteDeckTheme { AddCardScreen("val?", "", {}, {}, {}) }
}
