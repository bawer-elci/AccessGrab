package com.baverelci.accessgrab.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.baverelci.accessgrab.data.PreferencesManager
import com.baverelci.accessgrab.network.AccessGrabApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Uygulama kapalıyken veya kullanıcı uygulama içinde değilken bile periyodik olarak
 * sunucudaki yeni duyuruları ve güncellemeleri denetleyip özel sesli bildirim gönderen arka plan alıcısı.
 */
class BackgroundSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext

        schedulePeriodicSync(appContext)
        NotificationSyncService.startServiceSafely(appContext)
        NotificationSyncService.scheduleRestartAlarm(appContext, 25_000L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkServerNotifications(appContext)
            } catch (_: Exception) {
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        const val ACTION_BACKGROUND_SYNC = "com.baverelci.accessgrab.ACTION_BACKGROUND_SYNC"
        private const val REQUEST_CODE_ALARM = 4402

        fun schedulePeriodicSync(context: Context) {
            try {
                val appContext = context.applicationContext
                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(appContext, BackgroundSyncReceiver::class.java).apply {
                    action = ACTION_BACKGROUND_SYNC
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    REQUEST_CODE_ALARM,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAt = SystemClock.elapsedRealtime() + 20_000L
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }

        suspend fun checkServerNotifications(context: Context) {
            val appContext = context.applicationContext
            val prefs = PreferencesManager(appContext)

            // 1. Canlı Duyuru / Bildirim Kontrolü (Uygulama dışındakilere de bildirim düşer)
            try {
                val ann = AccessGrabApi.service.getAnnouncement()
                if (ann.active && ann.id.isNotBlank() && ann.message.isNotBlank()) {
                    if (ann.id != prefs.getLastNotifiedAnnouncementId()) {
                        prefs.setLastNotifiedAnnouncementId(ann.id)
                        UpdateNotificationHelper.showAnnouncementNotification(
                            context = appContext,
                            title = ann.title,
                            message = ann.message
                        )
                    }
                }
            } catch (_: Exception) {}

            // 2. Yeni Güncelleme Kontrolü
            if (prefs.isAutoUpdateEnabled.value) {
                try {
                    val info = AccessGrabApi.service.getLatestVersion()
                    val currentVersionCode = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            appContext.packageManager.getPackageInfo(appContext.packageName, 0).longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionCode.toLong()
                        }
                    } catch (_: Exception) {
                        1L
                    }

                    if (info.latestVersionCode > currentVersionCode &&
                        prefs.getLastNotifiedVersionCode() < info.latestVersionCode
                    ) {
                        prefs.setLastNotifiedVersionCode(info.latestVersionCode)
                        UpdateNotificationHelper.showUpdateNotification(
                            context = appContext,
                            versionName = info.latestVersionName,
                            changelog = info.changelog
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
