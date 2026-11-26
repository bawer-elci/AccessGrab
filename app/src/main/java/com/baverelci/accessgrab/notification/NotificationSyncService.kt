package com.baverelci.accessgrab.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
 * Akıllı Pil Tasarrufu Modu (ekran açıkken WakeLock'suz 10 sn, ekran kapalıyken 2.5 sn mikro
 * uyandırmalı 22 sn periyot ve çevrimdışı uyku) ile şarj tüketmeden anlık bildirim düşüren servis.
 */
class NotificationSyncService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        promoteToForegroundSafely()
        startSmartPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForegroundSafely()
        if (pollingJob?.isActive != true) {
            startSmartPolling()
        }
        scheduleRestartAlarm(applicationContext, 45_000L)
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

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun startSmartPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isScreenOn = pm?.isInteractive ?: true

                if (isNetworkAvailable(applicationContext)) {
                    var wakeLock: PowerManager.WakeLock? = null
                    try {
                        // Ekran zaten açıksa WakeLock'a hiç gerek yok (pil tasarrufu)
                        if (!isScreenOn) {
                            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AccessGrab:SyncPollLock")?.apply {
                                setReferenceCounted(false)
                                acquire(2_500L)
                            }
                        }
                        val shouldCheckVersion = (tickCount % 3 == 0)
                        BackgroundSyncReceiver.checkServerNotifications(
                            context = applicationContext,
                            checkVersion = shouldCheckVersion
                        )
                        tickCount++
                        if (tickCount % 4 == 0) {
                            scheduleRestartAlarm(applicationContext, 45_000L)
                        }
                    } catch (_: Exception) {
                    } finally {
                        try {
                            if (wakeLock?.isHeld == true) {
                                wakeLock.release()
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Akıllı bekleme süresi: Ekran açıkken 10 sn, ekran kilitliyken 22 sn (minimum pil tüketimi)
                val sleepDurationMs = if (isScreenOn) 10_000L else 22_000L
                delay(sleepDurationMs)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        scheduleRestartAlarm(applicationContext, 8_000L)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scheduleRestartAlarm(applicationContext, 8_000L)
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
                    scheduleRestartAlarm(appContext, 10_000L)
                }
            }
        }

        fun scheduleRestartAlarm(context: Context, delayMs: Long = 45_000L) {
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
