package com.baverelci.accessgrab.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baverelci.accessgrab.MainActivity

object DownloadNotificationHelper {

    private const val PROGRESS_CHANNEL_ID = "accessgrab_download_progress"
    private const val COMPLETE_CHANNEL_ID = "accessgrab_download_complete"
    private const val PROGRESS_NOTIFICATION_ID = 2001
    private const val COMPLETE_NOTIFICATION_ID = 2002

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val progressChannel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "Aktif İndirme İlerlemesi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Devam eden medya indirmelerinin anlık yüzdesini gösterir"
                setSound(null, null)
                enableVibration(false)
            }

            val completeChannel = NotificationChannel(
                COMPLETE_CHANNEL_ID,
                "Tamamlanan İndirmeler",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "İndirme tamamlandığında bildirim gönderir"
            }

            nm.createNotificationChannel(progressChannel)
            nm.createNotificationChannel(completeChannel)
        }
    }

    fun showProgressNotification(
        context: Context,
        title: String,
        format: String,
        progress: Int,
        isIndeterminate: Boolean,
        queueRemaining: Int = 0
    ) {
        ensureChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", 0)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val queueSuffix = if (queueRemaining > 0) " • Sırada: $queueRemaining" else ""
        val statusText = if (isIndeterminate) {
            "Sunucuda hazırlanıyor ($format)$queueSuffix"
        } else {
            "%$progress indirildi ($format)$queueSuffix"
        }

        val builder = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(statusText)
            .setProgress(100, progress.coerceIn(0, 100), isIndeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        try {
            NotificationManagerCompat.from(context).notify(PROGRESS_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showCompletedNotification(
        context: Context,
        title: String,
        format: String
    ) {
        ensureChannels(context)
        cancelProgressNotification(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", 1) // Kütüphane sekmesini aç
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, COMPLETE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("İndirme Tamamlandı ($format)")
            .setContentText("$title • Kütüphanede açmak için dokunun.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        try {
            NotificationManagerCompat.from(context).notify(COMPLETE_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {}
    }

    fun cancelProgressNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(PROGRESS_NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
