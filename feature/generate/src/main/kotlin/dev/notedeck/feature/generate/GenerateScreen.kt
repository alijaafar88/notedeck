package dev.notedeck.feature.generate

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.notedeck.core.ui.NoteDeckTheme
import dev.notedeck.domain.NotesPhoto
import dev.notedeck.domain.Summary
import dev.notedeck.domain.SummaryRoute
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GenerateRoute(
    deckName: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GenerateViewModel = hiltViewModel<GenerateViewModel, GenerateViewModel.Factory> { it.create(deckName) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The Photo Picker needs no storage permission; the learner shares one photo, nothing else.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { viewModel.onPhoto(loadNotesPhoto(context, uri)) }
    }
    val scanPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { viewModel.scan(loadNotesPhoto(context, uri)) }
    }
    GenerateScreen(
        state,
        onScan = { scanPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onSummarize = viewModel::summarize,
        onDownloadSummaries = viewModel::downloadSummaries,
        onNotesChange = viewModel::onNotesChange,
        onChooseProvider = viewModel::chooseProvider,
        onAddPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemovePhoto = { viewModel.onPhoto(null) },
        onGenerate = viewModel::generate,
        onStop = viewModel::stop,
        onEditFront = viewModel::editFront,
        onEditBack = viewModel::editBack,
        onRemove = viewModel::remove,
        onSave = viewModel::save,
        onStartOver = viewModel::startOver,
        onDone = onDone,
        modifier = modifier,
    )
}

/** A quarter of the photo's resolution is plenty to read notes, and far fewer bytes to upload and tokens to pay for. */
private suspend fun loadNotesPhoto(context: Context, uri: Uri): NotesPhoto? = withContext(Dispatchers.IO) {
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })
    } ?: return@withContext null
    val bytes = ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
    NotesPhoto(bytes)
}

@Composable
fun GenerateScreen(
    state: GenerateUiState,
    onNotesChange: (String) -> Unit,
    onChooseProvider: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    onEditFront: (Int, String) -> Unit,
    onEditBack: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onSave: () -> Unit,
    onStartOver: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onScan: () -> Unit = {},
    onSummarize: () -> Unit = {},
    onDownloadSummaries: () -> Unit = {},
) {
    Surface(modifier) {
        Column(
            Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Generate cards for ${state.deckName}", style = MaterialTheme.typography.titleLarge)
            when (state.phase) {
                GeneratePhase.Writing, GeneratePhase.Streaming -> {
                    WritingPart(state, onNotesChange, onChooseProvider, onAddPhoto, onRemovePhoto, onGenerate, onStop, onScan)
                    if (state.phase == GeneratePhase.Writing) SummaryPart(state, onSummarize, onDownloadSummaries)
                }
                GeneratePhase.Reviewing -> ReviewPart(state, onEditFront, onEditBack, onRemove, onSave, onStartOver)
                GeneratePhase.Saved -> {
                    Text("Saved ${state.savedCount} ${if (state.savedCount == 1) "card" else "cards"} to ${state.deckName}.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onDone) { Text("Done") }
                        OutlinedButton(onClick = onStartOver) { Text("Generate more") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WritingPart(
    state: GenerateUiState,
    onNotesChange: (String) -> Unit,
    onChooseProvider: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    onScan: () -> Unit,
) {
    val streaming = state.phase == GeneratePhase.Streaming
    Text("Model", style = MaterialTheme.typography.titleMedium)
    state.providers.forEach { provider ->
        Row(
            Modifier.fillMaxWidth().selectable(
                selected = provider.id == state.providerId,
                enabled = provider.available && !streaming,
                role = Role.RadioButton,
                onClick = { onChooseProvider(provider.id) },
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = provider.id == state.providerId, onClick = null, enabled = provider.available && !streaming)
            Text(if (provider.available) provider.name else "${provider.name} (not set up)")
        }
    }
    OutlinedTextField(
        value = state.notes,
        onValueChange = onNotesChange,
        label = { Text("Your notes") },
        minLines = 5,
        enabled = !streaming,
        modifier = Modifier.fillMaxWidth(),
    )
    // OCR runs on the phone, so the photo is never uploaded; only the text it finds joins the notes.
    OutlinedButton(onClick = onScan, enabled = !streaming && !state.scanning) {
        Text(if (state.scanning) "Reading the photo..." else "Scan notes from a photo")
    }
    if (state.provider?.acceptsPhotos == true) {
        if (state.hasPhoto) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Photo attached")
                TextButton(onClick = onRemovePhoto, enabled = !streaming) { Text("Remove photo") }
            }
        } else {
            OutlinedButton(onClick = onAddPhoto, enabled = !streaming) { Text("Add a photo of notes") }
        }
    }
    state.provider?.takeIf { it.id != "fake" }?.let {
        Text("Your notes${if (state.hasPhoto) " and photo" else ""} are sent to ${it.name} to draft cards.", style = MaterialTheme.typography.bodySmall)
    }
    if (streaming) {
        OutlinedButton(onClick = onStop) { Text("Stop") }
    } else {
        Button(onClick = onGenerate, enabled = state.canGenerate) { Text("Generate cards") }
    }
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (state.streamed.isNotEmpty()) {
        Card(Modifier.fillMaxWidth()) {
            Text(
                state.streamed,
                Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Every state gets words: where a summary would come from, or why there can't be one. */
@Composable
private fun SummaryPart(state: GenerateUiState, onSummarize: () -> Unit, onDownloadSummaries: () -> Unit) {
    Text("Summary", style = MaterialTheme.typography.titleMedium)
    when (state.summaryRoute) {
        null -> Unit
        SummaryRoute.OnDevice, SummaryRoute.Cloud -> Button(onClick = onSummarize, enabled = state.canSummarize) {
            val where = if (state.summaryRoute == SummaryRoute.OnDevice) "on this phone" else "in the cloud"
            Text(if (state.summarizing) "Summarising..." else "Summarize $where")
        }
        SummaryRoute.NeedsDownload -> OutlinedButton(onClick = onDownloadSummaries) { Text("Download on-device summaries (one time)") }
        SummaryRoute.Downloading -> Text("Getting on-device summaries ready...")
        SummaryRoute.NotOnThisPhone -> Text(
            "Summaries need Gemini Nano, which this phone doesn't have, or cloud AI turned on in Settings.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    state.summary?.let { summary ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (summary.onDevice) "AI summary, made on this phone" else "AI summary, made in the cloud", style = MaterialTheme.typography.labelMedium)
                Text(summary.text, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}

@Composable
private fun ReviewPart(
    state: GenerateUiState,
    onEditFront: (Int, String) -> Unit,
    onEditBack: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onSave: () -> Unit,
    onStartOver: () -> Unit,
) {
    Text("Check each card. Edit or remove any that are wrong, then save.", style = MaterialTheme.typography.bodyMedium)
    state.cards.forEachIndexed { index, card ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(card.front, { onEditFront(card.key, it) }, label = { Text("Front ${index + 1}") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(card.back, { onEditBack(card.key, it) }, label = { Text("Back ${index + 1}") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { onRemove(card.key) }) { Text("Remove card ${index + 1}") }
            }
        }
    }
    val count = state.cards.count { it.complete }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onSave, enabled = state.canSave) { Text("Save $count ${if (count == 1) "card" else "cards"}") }
        OutlinedButton(onClick = onStartOver) { Text("Start over") }
    }
}

private val previewProviders = listOf(
    ProviderChoice("gemini", "Gemini", available = false, acceptsPhotos = true),
    ProviderChoice("claude", "Claude", available = true, acceptsPhotos = false),
)

@Composable
private fun PreviewScreen(state: GenerateUiState) = NoteDeckTheme {
    GenerateScreen(state, {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}, {})
}

@Preview(name = "Streaming", showBackground = true)
@Preview(name = "Streaming, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GenerateStreamingPreview() = PreviewScreen(
    GenerateUiState(
        deckName = "Coroutines",
        providers = previewProviders,
        providerId = "claude",
        notes = "Flow: a cold stream",
        phase = GeneratePhase.Streaming,
        streamed = "[{\"front\": \"Is a Flow hot",
    ),
)

@Preview(name = "Summary from the cloud", showBackground = true)
@Composable
private fun GenerateSummaryPreview() = PreviewScreen(
    GenerateUiState(
        deckName = "Coroutines",
        providers = previewProviders,
        providerId = "claude",
        notes = "Flows are cold. collect starts them.",
        summaryRoute = SummaryRoute.Cloud,
        summary = Summary("- Flows are cold\n- collect starts them", onDevice = false),
    ),
)

@Preview(name = "No summaries on this phone", showBackground = true)
@Composable
private fun GenerateNoSummaryPreview() = PreviewScreen(
    GenerateUiState(deckName = "Coroutines", providers = previewProviders, providerId = "claude", summaryRoute = SummaryRoute.NotOnThisPhone),
)

@Preview(name = "Reviewing", showBackground = true)
@Composable
private fun GenerateReviewPreview() = PreviewScreen(
    GenerateUiState(
        deckName = "Coroutines",
        providers = previewProviders,
        providerId = "claude",
        phase = GeneratePhase.Reviewing,
        cards = listOf(EditableCard(0, "Is a Flow hot or cold?", "Cold"), EditableCard(1, "What starts a cold flow?", "")),
    ),
)
