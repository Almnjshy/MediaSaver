package com.mediasaver.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mediasaver.app.data.DefaultQuality
import com.mediasaver.app.data.DownloadEntity
import com.mediasaver.app.data.DownloadStatus
import com.mediasaver.app.domain.FormatOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialSharedText: String? = null,
    onOpenSettings: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    var linkText by remember { mutableStateOf(initialSharedText ?: "") }
    val probeState by viewModel.probeState.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val defaultQuality by viewModel.defaultQuality.collectAsState()

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            viewModel.onLinkSubmitted(initialSharedText)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MediaSaver") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = linkText,
                onValueChange = { linkText = it },
                label = { Text("الصق رابط الفيديو أو المنشور هنا") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { viewModel.onLinkSubmitted(linkText) },
                modifier = Modifier.fillMaxWidth(),
                enabled = linkText.isNotBlank() && probeState !is ProbeState.Loading
            ) {
                Text("تحليل الرابط")
            }

            Spacer(Modifier.height(16.dp))

            when (val state = probeState) {
                is ProbeState.Loading -> Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is ProbeState.Error -> Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                is ProbeState.Success -> FormatPickerCard(
                    state = state,
                    defaultQuality = defaultQuality,
                    onFormatSelected = { format -> viewModel.startDownload(state, format) }
                )

                ProbeState.Idle -> {}
            }

            Spacer(Modifier.height(24.dp))
            Text("سجل التنزيلات", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(history, key = { it.id }) { item ->
                    HistoryRow(item)
                }
            }
        }
    }
}

/** Best-effort match between the user's preferred default quality and this link's
 * actually-available formats — used only to highlight a suggestion, never to filter options out. */
private fun recommendedFormatId(formats: List<FormatOption>, quality: DefaultQuality): String? =
    when (quality) {
        DefaultQuality.AUDIO_ONLY -> formats.firstOrNull { it.isAudioOnly }?.formatId
        DefaultQuality.HD1080 -> formats.firstOrNull { "1080" in it.label }?.formatId
        DefaultQuality.HD720 -> formats.firstOrNull { "720" in it.label }?.formatId
        DefaultQuality.BEST -> formats.firstOrNull { !it.isAudioOnly }?.formatId ?: formats.firstOrNull()?.formatId
    }

@Composable
private fun FormatPickerCard(
    state: ProbeState.Success,
    defaultQuality: DefaultQuality,
    onFormatSelected: (FormatOption) -> Unit
) {
    val recommendedId = remember(state.media.formats, defaultQuality) {
        recommendedFormatId(state.media.formats, defaultQuality)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.media.thumbnailUrl != null) {
                    AsyncImage(
                        model = state.media.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(state.platform.displayName, style = MaterialTheme.typography.labelMedium)
                    Text(state.media.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("اختر الجودة:", style = MaterialTheme.typography.labelLarge)

            state.media.formats.forEach { format ->
                val isRecommended = format.formatId == recommendedId
                val label = format.label + if (isRecommended) " · موصى به" else ""

                if (isRecommended) {
                    Button(
                        onClick = { onFormatSelected(format) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onFormatSelected(format) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: DownloadEntity) {
    val statusLabel = when (item.status) {
        DownloadStatus.QUEUED -> "في الانتظار"
        DownloadStatus.RUNNING -> "جارٍ التنزيل — ${(item.progress * 100).toInt()}%"
        DownloadStatus.DONE -> "اكتمل"
        DownloadStatus.FAILED -> "فشل"
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        ListItem(
            headlineContent = { Text(item.title, maxLines = 1) },
            supportingContent = { Text("${item.platform} · $statusLabel") },
            leadingContent = {
                if (item.thumbnailUrl != null) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        )
        if (item.status == DownloadStatus.RUNNING) {
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    }
}
