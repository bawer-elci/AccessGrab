package com.baverelci.accessgrab.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baverelci.accessgrab.MainActivity
import com.baverelci.accessgrab.accessibility.AccessibilityHelper

object UpdateNotificationHelper {

    const val CHANNEL_ID = "accessgrab_updates_melody"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Uygulama Güncellemeleri (Melodili)"
            val descriptionText = "AccessGrab yeni sürüm çıktığında sesli bildirim gönderir"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(soundUri, audioAttrs)
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUpdateNotification(
        context: Context,
        versionName: String,
        changelog: String
    ) {
        createNotificationChannel(context)
        AccessibilityHelper.playUpdateMelody(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_TAB", 2) // Ayarlar sekmesini aç
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("AccessGrab Güncellemesi Mevcut (v$versionName)")
            .setContentText("Yeni özellikler ve iyileştirmeler için dokunun.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Yeni sürüm: v$versionName\n$changelog"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS izni henüz verilmemişse sessizce atla
        }
    }
}
