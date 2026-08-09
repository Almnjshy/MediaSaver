package com.mediasaver.app.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mediasaver.app.data.DownloadDao
import com.mediasaver.app.data.DownloadStatus
import com.mediasaver.app.data.MediaKind
import com.mediasaver.app.data.SettingsRepository
import com.mediasaver.app.domain.MediaExtractor
import com.mediasaver.app.util.DownloadNotifications
import com.mediasaver.app.util.MediaStoreSaver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val extractor: MediaExtractor,
    private val dao: DownloadDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    private val notificationId =
        NOTIFICATION_ID_BASE + (inputData.getLong(KEY_DOWNLOAD_ID, 0L) % 1000).toInt()

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val formatId = inputData.getString(KEY_FORMAT_ID) ?: "best"
        val isAudioOnly = inputData.getBoolean(KEY_AUDIO_ONLY, false)
        val title = inputData.getString(KEY_TITLE) ?: "تنزيل"

        val entity = dao.getById(downloadId)

        setForeground(createForegroundInfo(title, progressPercent = 0, indeterminate = true))
        entity?.let { dao.update(it.copy(status = DownloadStatus.RUNNING)) }

        val tempDir = File(applicationContext.cacheDir, "downloads")
        var lastReportedPercent = -1

        return try {
            val file = extractor.download(url, formatId, tempDir) { progress, _ ->
                val percent = progress.toInt().coerceIn(0, 100)
                setProgressAsync(workDataOf(KEY_PROGRESS to progress))
                if (percent != lastReportedPercent) {
                    lastReportedPercent = percent
                    setForegroundAsync(createForegroundInfo(title, percent, indeterminate = false))
                    entity?.let {
                        // The download callback runs on this worker's own coroutine, so a
                        // blocking bridge here is safe — it's not blocking the UI/main thread.
                        kotlinx.coroutines.runBlocking {
                            dao.update(it.copy(progress = progress / 100f))
                        }
                    }
                }
            }

            val kind = if (isAudioOnly) MediaKind.AUDIO else MediaKind.VIDEO
            val useSubfolder = settingsRepository.settings.first().saveToMoviesFolder
            val savedUri = MediaStoreSaver.saveToGallery(applicationContext, file, kind, useSubfolder)
            file.delete()

            entity?.let {
                dao.update(
                    it.copy(
                        status = DownloadStatus.DONE,
                        filePath = savedUri?.toString(),
                        progress = 1f
                    )
                )
            }
            notifyResult(title, success = true)

            Result.success(workDataOf(KEY_RESULT_URI to savedUri?.toString()))
        } catch (e: Exception) {
            entity?.let { dao.update(it.copy(status = DownloadStatus.FAILED)) }
            notifyResult(title, success = false)
            Result.retry()
        }
    }

    private fun notifyResult(title: String, success: Boolean) {
        val manager = applicationContext.getSystemService(android.app.NotificationManager::class.java)
        manager.notify(
            notificationId,
            DownloadNotifications.buildResultNotification(applicationContext, title, success)
        )
    }

    private fun createForegroundInfo(
        title: String,
        progressPercent: Int,
        indeterminate: Boolean
    ): ForegroundInfo {
        val notification: Notification = DownloadNotifications.buildProgressNotification(
            applicationContext, title, progressPercent, indeterminate
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_URL = "url"
        const val KEY_FORMAT_ID = "format_id"
        const val KEY_AUDIO_ONLY = "audio_only"
        const val KEY_TITLE = "title"
        const val KEY_PROGRESS = "progress"
        const val KEY_RESULT_URI = "result_uri"
        private const val NOTIFICATION_ID_BASE = DownloadNotifications.NOTIFICATION_ID_BASE
    }
}
