package com.mediasaver.app.util

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.mediasaver.app.data.MediaKind
import java.io.File

object MediaStoreSaver {

    /** Copies a downloaded file into the public gallery via MediaStore (no storage permission needed on API 29+).
     * @param useSubfolder true saves under a "MediaSaver" subfolder (e.g. Movies/MediaSaver);
     *   false saves directly into the root media folder (e.g. Movies/), matching stock camera/download apps.
     */
    fun saveToGallery(
        context: Context,
        file: File,
        kind: MediaKind,
        useSubfolder: Boolean = true
    ): android.net.Uri? {
        val (collection, rootFolder, mime) = when (kind) {
            MediaKind.VIDEO -> Triple(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Movies", "video/mp4")
            MediaKind.IMAGE -> Triple(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Pictures", "image/jpeg")
            MediaKind.AUDIO -> Triple(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "Music", "audio/mp4")
        }
        val relativePath = if (useSubfolder) "$rootFolder/MediaSaver" else rootFolder

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return null

        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return uri
    }
}
