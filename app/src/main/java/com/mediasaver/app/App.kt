package com.mediasaver.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mediasaver.app.util.DownloadNotifications
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything here runs strictly on-device.
 * YoutubeDL.init() unpacks the yt-dlp python runtime + binaries that ship
 * inside the APK (no network call to any backend of ours, no subscription).
 * FFmpeg.init() does the same for local audio/video remuxing.
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        DownloadNotifications.ensureChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            YoutubeDL.getInstance().init(this@App)
            FFmpeg.getInstance().init(this@App)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
