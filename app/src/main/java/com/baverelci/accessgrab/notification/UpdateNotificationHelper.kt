package com.baverelci.accessgrab.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baverelci.accessgrab.MainActivity
import com.baverelci.accessgrab.R
import com.baverelci.accessgrab.accessibility.AccessibilityHelper

object UpdateNotificationHelper {

    const val UPDATE_CHANNEL_ID = "accessgrab_updates_chime_v2"
    const val ANNOUNCEMENT_CHANNEL_ID = "accessgrab_announcements_chime_v2"
    const val UPDATE_NOTIFICATION_ID = 1001
    const val ANNOUNCEMENT_NOTIFICATION_ID = 1005

    private fun getCustomChimeUri(context: Context): Uri {
        return Uri.parse("android.resource://${context.packageName}/${R.raw.accessgrab_chime}")
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = getCustomChimeUri(context)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "Uygulama Güncellemeleri (Özel Melodili)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AccessGrab yeni sürüm çıktığında özel melodiyle bildirim gönderir"
                setSound(soundUri, audioAttrs)
                enableVibration(true)
            }

            val announcementChannel = NotificationChannel(
                ANNOUNCEMENT_CHANNEL_ID,
                "AccessGrab Canlı Duyuru ve Bildirimler",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Geliştiriciden gelen önemli duyuruları uygulama kapalıyken bile özel sesle bildirir"
                setSound(soundUri, audioAttrs)
                enableVibration(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(updateChannel)
            notificationManager.createNotificationChannel(announcementChannel)
        }
    }

    fun showUpdateNotification(
        context: Context,
        versionName: String,
        changelog: String
    ) {
        createNotificationChannels(context)
        AccessibilityHelper.playCustomNotificationChime(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", 2) // Ayarlar sekmesini aç
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("AccessGrab Güncellemesi Mevcut (v$versionName)")
            .setContentText("Yeni özellikler ve iyileştirmeler için dokunun.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Yeni sürüm: v$versionName\n$changelog"))
            .setSound(getCustomChimeUri(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(UPDATE_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {}
    }

    fun showAnnouncementNotification(
        context: Context,
        title: String,
        message: String
    ) {
        createNotificationChannels(context)
        AccessibilityHelper.playCustomNotificationChime(context)

        val cleanTitle = title.ifBlank { "AccessGrab Duyurusu" }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ANNOUNCEMENT_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ANNOUNCEMENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(cleanTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle(cleanTitle))
            .setSound(getCustomChimeUri(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(ANNOUNCEMENT_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {}
    }
}
