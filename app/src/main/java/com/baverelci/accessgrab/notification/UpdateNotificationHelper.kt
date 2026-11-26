package com.baverelci.accessgrab.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baverelci.accessgrab.MainActivity
import com.baverelci.accessgrab.R
import com.baverelci.accessgrab.accessibility.AccessibilityHelper

object UpdateNotificationHelper {

    const val UPDATE_CHANNEL_ID = "accessgrab_updates_chime_v3"
    const val ANNOUNCEMENT_CHANNEL_ID = "accessgrab_announcements_chime_v3"
    const val SYNC_GUARD_CHANNEL_ID = "accessgrab_bg_guard_silent_v1"

    const val UPDATE_NOTIFICATION_ID = 1001
    const val ANNOUNCEMENT_NOTIFICATION_ID = 2000
    const val FOREGROUND_GUARD_NOTIFICATION_ID = 4401

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
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val announcementChannel = NotificationChannel(
                ANNOUNCEMENT_CHANNEL_ID,
                "AccessGrab Canlı Duyuru ve Bildirimler",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Geliştiriciden gelen önemli duyuruları uygulama kapalıyken bile özel sesle bildirir"
                setSound(soundUri, audioAttrs)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val guardChannel = NotificationChannel(
                SYNC_GUARD_CHANNEL_ID,
                "Arka Plan Bildirim Servisi (Sessiz)",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Uygulama kapalıyken anlık bildirim ve güncellemeleri yakalamak için sessiz çalışır"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(updateChannel)
            notificationManager.createNotificationChannel(announcementChannel)
            notificationManager.createNotificationChannel(guardChannel)
        }
    }

    fun buildForegroundGuardNotification(context: Context): Notification {
        createNotificationChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            FOREGROUND_GUARD_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SYNC_GUARD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle("AccessGrab Bildirim Servisi Aktif")
            .setContentText("Yeni güncellemeler ve duyurular arka planda dinleniyor")
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post(block)
        }
    }

    fun showUpdateNotification(
        context: Context,
        versionName: String,
        changelog: String,
        versionCode: Int = 0
    ) {
        val appContext = context.applicationContext
        runOnMainThread {
            createNotificationChannels(appContext)
            AccessibilityHelper.playCustomNotificationChime(appContext)

            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_TAB", 2) // Ayarlar sekmesini aç
                putExtra("AUTO_CHECK_UPDATE", true) // Doğrudan güncelleme kurulum penceresini aç
            }

            val notifId = UPDATE_NOTIFICATION_ID + (versionCode and 0xFF)
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(appContext, UPDATE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Yeni AccessGrab Güncellemesi Mevcut (v$versionName)")
                .setContentText("Yeni sürümü doğrudan indirmek ve kurmak için dokunun.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle("AccessGrab v$versionName Yayında!")
                        .bigText("Yeni sürüm hazır! Doğrudan kurmak için dokunun.\n\nYenilikler:\n$changelog")
                )
                .setSound(getCustomChimeUri(appContext))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                val manager = NotificationManagerCompat.from(appContext)
                manager.notify(notifId, builder.build())
            } catch (_: SecurityException) {}
        }
    }

    fun showAnnouncementNotification(
        context: Context,
        title: String,
        message: String,
        announcementId: String = ""
    ) {
        val appContext = context.applicationContext
        runOnMainThread {
            createNotificationChannels(appContext)
            AccessibilityHelper.playCustomNotificationChime(appContext)

            val cleanTitle = title.ifBlank { "AccessGrab Duyurusu" }
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val dynamicOffset = if (announcementId.isNotBlank()) (announcementId.hashCode() and 0x3FFF) else 5
            val notifId = ANNOUNCEMENT_NOTIFICATION_ID + dynamicOffset

            val pendingIntent = PendingIntent.getActivity(
                appContext,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(appContext, ANNOUNCEMENT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(cleanTitle)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle(cleanTitle))
                .setSound(getCustomChimeUri(appContext))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                val manager = NotificationManagerCompat.from(appContext)
                manager.notify(notifId, builder.build())
            } catch (_: SecurityException) {}
        }
    }
}
