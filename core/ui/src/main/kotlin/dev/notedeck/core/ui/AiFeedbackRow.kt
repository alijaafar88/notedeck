package dev.notedeck.core.ui

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Under every AI result: was it helpful, and a way to report harmful output without leaving the app
 * (Google Play requires that for AI-generated content). Once rated, it shows [thanks] instead.
 */
@Composable
fun AiFeedbackRow(
    thanks: String?,
    onHelpful: () -> Unit,
    onNotHelpful: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (thanks != null) {
        Text(thanks, modifier, style = MaterialTheme.typography.bodySmall)
        return
    }
    FlowRow(modifier) {
        TextButton(onClick = onHelpful) { Text("Helpful") }
        TextButton(onClick = onNotHelpful) { Text("Not helpful") }
        TextButton(onClick = onReport) { Text("Report") }
    }
}

@Preview(showBackground = true)
@Composable
private fun AiFeedbackRowPreview() = NoteDeckTheme { AiFeedbackRow(thanks = null, onHelpful = {}, onNotHelpful = {}, onReport = {}) }
