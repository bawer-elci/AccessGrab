package com.baverelci.accessgrab.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Uygulama arka plandayken, ekran kapalıyken veya son uygulamalardan kapatıldığında bile
 * Android tarafından öldürülmemek için sessiz Foreground Service olarak çalışan,
 * her 4 saniyede bir hem panel bildirimlerini hem de yeni uygulama güncellemelerini
 * denetleyip özel melodiyle (accessgrab_chime.wav) bildirim düşüren servis.
 */
class NotificationSyncService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        promoteToForegroundSafely()
        startFastPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForegroundSafely()
        if (pollingJob?.isActive != true) {
            startFastPolling()
        }
        scheduleRestartAlarm(applicationContext, 20_000L)
        return START_STICKY
    }

    private fun promoteToForegroundSafely() {
        try {
            val notification = UpdateNotificationHelper.buildForegroundGuardNotification(applicationContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    UpdateNotificationHelper.FOREGROUND_GUARD_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    UpdateNotificationHelper.FOREGROUND_GUARD_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(
                    UpdateNotificationHelper.FOREGROUND_GUARD_NOTIFICATION_ID,
                    notification
                )
            }
        } catch (_: Exception) {}
    }

    private fun startFastPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                var wakeLock: PowerManager.WakeLock? = null
                try {
                    val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AccessGrab:SyncPollLock")?.apply {
                        setReferenceCounted(false)
                        acquire(10_000L)
                    }
                    BackgroundSyncReceiver.checkServerNotifications(applicationContext)
                    tickCount++
                    if (tickCount % 5 == 0) {
                        scheduleRestartAlarm(applicationContext, 20_000L)
                    }
                } catch (_: Exception) {
                } finally {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (_: Exception) {}
                }
                delay(4_000L) // Her 4 saniyede bir canlı duyuru ve yeni sürüm kontrolü
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        scheduleRestartAlarm(applicationContext, 5_000L)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scheduleRestartAlarm(applicationContext, 5_000L)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startServiceSafely(context: Context) {
            val appContext = context.applicationContext
            try {
                val intent = Intent(appContext, NotificationSyncService::class.java)
                ContextCompat.startForegroundService(appContext, intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(appContext, NotificationSyncService::class.java)
                    appContext.startService(intent)
                } catch (_: Exception) {
                    scheduleRestartAlarm(appContext, 5_000L)
                }
            }
        }

        fun scheduleRestartAlarm(context: Context, delayMs: Long = 15_000L) {
            try {
                val appContext = context.applicationContext
                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(appContext, BackgroundSyncReceiver::class.java).apply {
                    action = BackgroundSyncReceiver.ACTION_BACKGROUND_SYNC
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    4409,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val triggerAt = SystemClock.elapsedRealtime() + delayMs
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } catch (_: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            } catch (_: Exception) {}
        }
    }
}
