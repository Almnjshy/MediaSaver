package com.mediasaver.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object DownloadNotifications {
    const val CHANNEL_ID = "downloads"
    const val NOTIFICATION_ID_BASE = 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "التنزيلات",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدّم تنزيل الفيديو والصوت"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progressPercent: Int,
        indeterminate: Boolean
    ) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(if (indeterminate) "جارٍ التحضير…" else "$progressPercent%")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setProgress(100, progressPercent, indeterminate)
        .build()

    fun buildResultNotification(
        context: Context,
        title: String,
        success: Boolean
    ) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(if (success) "اكتمل التنزيل" else "فشل التنزيل")
        .setSmallIcon(
            if (success) android.R.drawable.stat_sys_download_done
            else android.R.drawable.stat_notify_error
        )
        .setAutoCancel(true)
        .build()
}
