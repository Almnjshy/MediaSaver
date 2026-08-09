package com.mediasaver.app.domain

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class FormatOption(
    val formatId: String,
    val label: String,      // e.g. "1080p mp4" or "audio only (m4a)"
    val isAudioOnly: Boolean,
    val fileSizeBytes: Long?
)

data class ExtractedMedia(
    val title: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val formats: List<FormatOption>
)

/**
 * Thin wrapper around yt-dlp running locally on the device.
 * No calls to any backend server of ours — every request goes straight
 * from the user's phone to the source URL.
 */
@Singleton
class MediaExtractor @Inject constructor() {

    suspend fun probe(url: String): ExtractedMedia = withContext(Dispatchers.IO) {
        val info: VideoInfo = YoutubeDL.getInstance().getInfo(url)
        val formats = info.formats.orEmpty().map { f ->
            FormatOption(
                formatId = f.formatId ?: "best",
                label = buildLabel(f.formatNote, f.ext, f.vcodec),
                isAudioOnly = (f.vcodec == "none" || f.vcodec.isNullOrEmpty()),
                fileSizeBytes = f.fileSize.takeIf { it > 0 }
            )
        }
        ExtractedMedia(
            title = info.title ?: "media",
            thumbnailUrl = info.thumbnail,
            durationSeconds = info.duration.takeIf { it > 0 },
            formats = formats.ifEmpty {
                listOf(FormatOption("best", "أفضل جودة متاحة", false, null))
            }
        )
    }

    suspend fun download(
        url: String,
        formatId: String,
        outputDir: File,
        onProgress: (percent: Float, etaSeconds: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (!outputDir.exists()) outputDir.mkdirs()

        val request = YoutubeDLRequest(url).apply {
            addOption("-f", formatId)
            addOption("-o", "${outputDir.absolutePath}/%(title).80s.%(ext)s")
            addOption("--no-mtime")
        }

        YoutubeDL.getInstance().execute(request, null) { progress, etaSeconds, _ ->
            onProgress(progress, etaSeconds)
        }

        outputDir.listFiles()
            ?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("لم يتم العثور على الملف بعد التنزيل")
    }

    private fun buildLabel(note: String?, ext: String?, vcodec: String?): String {
        val isAudioOnly = vcodec == "none" || vcodec.isNullOrEmpty()
        return when {
            isAudioOnly -> "صوت فقط (${ext ?: "m4a"})"
            !note.isNullOrBlank() -> "$note (${ext ?: "mp4"})"
            else -> "فيديو (${ext ?: "mp4"})"
        }
    }
}
