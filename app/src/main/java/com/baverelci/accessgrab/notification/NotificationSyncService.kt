package com.baverelci.accessgrab.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Uygulama arka plandayken veya kapatıldığında bile panelden gönderilen bildirimleri
 * 8 saniye içinde yakalayıp özel bildirim sesiyle (accessgrab_chime.wav) kullanıcıya ulaştıran
 * hafif arka plan senkronizasyon servisi.
 */
class NotificationSyncService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startFastPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (pollingJob?.isActive != true) {
            startFastPolling()
        }
        return START_STICKY
    }

    private fun startFastPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    BackgroundSyncReceiver.checkServerNotifications(applicationContext)
                } catch (_: Exception) {}
                delay(8_000L) // Her 8 saniyede bir yeni bildirim kontrolü
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Kullanıcı uygulamayı son uygulamalardan kaydırıp kapatsa bile 10 sn sonra servisi yeniden uyandır
        scheduleRestartAlarm(applicationContext, 10_000L)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scheduleRestartAlarm(applicationContext, 15_000L)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startServiceSafely(context: Context) {
            try {
                val appContext = context.applicationContext
                val intent = Intent(appContext, NotificationSyncService::class.java)
                appContext.startService(intent)
            } catch (_: Exception) {
                // Android 8+ arka plan kısıtlaması durumunda AlarmManager ile uyandır
                scheduleRestartAlarm(context.applicationContext, 10_000L)
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
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } catch (_: Exception) {}
        }
    }
}
