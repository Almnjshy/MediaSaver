package com.mediasaver.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.mediasaver.app.data.DownloadDao
import com.mediasaver.app.data.DownloadEntity
import com.mediasaver.app.data.DownloadStatus
import com.mediasaver.app.data.MediaKind
import com.mediasaver.app.data.SettingsRepository
import com.mediasaver.app.domain.ExtractedMedia
import com.mediasaver.app.domain.FormatOption
import com.mediasaver.app.domain.MediaExtractor
import com.mediasaver.app.util.Platform
import com.mediasaver.app.util.UrlDetector
import com.mediasaver.app.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProbeState {
    data object Idle : ProbeState
    data object Loading : ProbeState
    data class Success(val url: String, val platform: Platform, val media: ExtractedMedia) : ProbeState
    data class Error(val message: String) : ProbeState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val extractor: MediaExtractor,
    private val dao: DownloadDao,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _probeState = MutableStateFlow<ProbeState>(ProbeState.Idle)
    val probeState: StateFlow<ProbeState> = _probeState.asStateFlow()

    val history = dao.observeAll()

    val defaultQuality = settingsRepository.settings
        .map { it.defaultQuality }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.mediasaver.app.data.DefaultQuality.BEST)

    fun onLinkSubmitted(rawText: String) {
        val url = UrlDetector.extractUrl(rawText)
        if (url == null) {
            _probeState.value = ProbeState.Error("لم يتم العثور على رابط صالح")
            return
        }
        val platform = UrlDetector.detectPlatform(url)
        _probeState.value = ProbeState.Loading

        viewModelScope.launch {
            try {
                val media = extractor.probe(url)
                _probeState.value = ProbeState.Success(url, platform, media)
            } catch (e: Exception) {
                _probeState.value = ProbeState.Error(
                    "تعذّر استخراج الرابط — قد تكون المنصة غيّرت بنيتها التقنية. (${e.message ?: "خطأ غير معروف"})"
                )
            }
        }
    }

    /** Inserts a QUEUED row up front so the history list reflects the download immediately,
     * then hands the row's id to the worker so it can update status/progress as it runs. */
    fun startDownload(state: ProbeState.Success, format: FormatOption) {
        viewModelScope.launch {
            val kind = if (format.isAudioOnly) MediaKind.AUDIO else MediaKind.VIDEO
            val entity = DownloadEntity(
                sourceUrl = state.url,
                platform = state.platform.displayName,
                title = state.media.title,
                filePath = null,
                thumbnailUrl = state.media.thumbnailUrl,
                kind = kind,
                status = DownloadStatus.QUEUED
            )
            val downloadId = dao.insert(entity)

            val data = workDataOf(
                DownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                DownloadWorker.KEY_URL to state.url,
                DownloadWorker.KEY_FORMAT_ID to format.formatId,
                DownloadWorker.KEY_AUDIO_ONLY to format.isAudioOnly,
                DownloadWorker.KEY_TITLE to state.media.title
            )
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .addTag(workTagFor(downloadId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(getApplication()).enqueue(request)
            _probeState.value = ProbeState.Idle
        }
    }

    companion object {
        private const val WORK_TAG_PREFIX = "download_"
        fun workTagFor(downloadId: Long) = "$WORK_TAG_PREFIX$downloadId"
    }

    fun reset() {
        _probeState.value = ProbeState.Idle
    }
}
