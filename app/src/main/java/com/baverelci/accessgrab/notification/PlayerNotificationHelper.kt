package com.baverelci.accessgrab.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baverelci.accessgrab.MainActivity
import com.baverelci.accessgrab.player.InAppPlayerManager

class PlayerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            PlayerNotificationHelper.ACTION_TOGGLE -> InAppPlayerManager.togglePlayPause()
            PlayerNotificationHelper.ACTION_NEXT -> InAppPlayerManager.playNext()
            PlayerNotificationHelper.ACTION_STOP -> InAppPlayerManager.stop()
        }
    }
}

object PlayerNotificationHelper {

    private const val CHANNEL_ID = "accessgrab_media_player"
    private const val NOTIFICATION_ID = 3001

    const val ACTION_TOGGLE = "com.baverelci.accessgrab.PLAYER_TOGGLE"
    const val ACTION_NEXT = "com.baverelci.accessgrab.PLAYER_NEXT"
    const val ACTION_STOP = "com.baverelci.accessgrab.PLAYER_STOP"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medya Oynatıcı Kontrolleri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka planda ve ekran kilitliyken medya oynatıcı kontrollerini gösterir"
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun showOrUpdateNotification(
        context: Context,
        title: String,
        isPlaying: Boolean,
        positionText: String,
        durationText: String
    ) {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", 1)
        }
        val contentPending = PendingIntent.getActivity(
            context,
            3001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePending = PendingIntent.getBroadcast(
            context,
            3002,
            Intent(context, PlayerActionReceiver::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPending = PendingIntent.getBroadcast(
            context,
            3003,
            Intent(context, PlayerActionReceiver::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getBroadcast(
            context,
            3004,
            Intent(context, PlayerActionReceiver::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusLabel = if (isPlaying) "Oynatılıyor" else "Duraklatıldı"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(
                if (isPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            )
            .setContentTitle(title)
            .setContentText("$statusLabel • $positionText / $durationText")
            .setContentIntent(contentPending)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Duraklat" else "Oynat",
                togglePending
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Sonraki",
                nextPending
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Kapat",
                stopPending
            )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {}
    }

    fun cancelNotification(context: Context?) {
        if (context == null) return
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
