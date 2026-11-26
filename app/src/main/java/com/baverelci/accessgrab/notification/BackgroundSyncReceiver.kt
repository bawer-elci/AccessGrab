package com.baverelci.accessgrab.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.baverelci.accessgrab.data.PreferencesManager
import com.baverelci.accessgrab.network.AccessGrabApi
import com.baverelci.accessgrab.network.AnnouncementInfo
import com.baverelci.accessgrab.network.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Uygulama kapalıyken veya kullanıcı uygulama içinde değilken bile periyodik olarak
 * sunucudaki yeni duyuruları ve güncellemeleri denetleyip özel sesli bildirim gönderen arka plan alıcısı.
 */
class BackgroundSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext

        schedulePeriodicSync(appContext)
        NotificationSyncService.startServiceSafely(appContext)
        NotificationSyncService.scheduleRestartAlarm(appContext, 15_000L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var wakeLock: PowerManager.WakeLock? = null
            try {
                val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AccessGrab:ReceiverSyncLock")?.apply {
                    setReferenceCounted(false)
                    acquire(12_000L)
                }
                checkServerNotifications(appContext)
            } catch (_: Exception) {
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {}
                try {
                    pendingResult.finish()
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        const val ACTION_BACKGROUND_SYNC = "com.baverelci.accessgrab.ACTION_BACKGROUND_SYNC"
        private const val REQUEST_CODE_ALARM = 4402
        private val syncMutex = Mutex()

        private val _latestAnnouncementFlow = MutableStateFlow<AnnouncementInfo?>(null)
        val latestAnnouncementFlow: StateFlow<AnnouncementInfo?> = _latestAnnouncementFlow.asStateFlow()

        private val _latestUpdateFlow = MutableStateFlow<UpdateInfo?>(null)
        val latestUpdateFlow: StateFlow<UpdateInfo?> = _latestUpdateFlow.asStateFlow()

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

                val triggerAt = SystemClock.elapsedRealtime() + 15_000L
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
            syncMutex.withLock {
                val prefs = PreferencesManager(appContext)

                // 1. Canlı Duyuru / Bildirim Kontrolü (Uygulama dışındakilere de anında bildirim düşer)
                try {
                    val ann = AccessGrabApi.service.getAnnouncement()
                    if (ann.active && ann.id.isNotBlank() && ann.message.isNotBlank()) {
                        _latestAnnouncementFlow.value = ann
                        if (ann.id != prefs.getLastNotifiedAnnouncementId()) {
                            prefs.setLastNotifiedAnnouncementId(ann.id)
                            UpdateNotificationHelper.showAnnouncementNotification(
                                context = appContext,
                                title = ann.title,
                                message = ann.message,
                                announcementId = ann.id
                            )
                        }
                    } else {
                        _latestAnnouncementFlow.value = null
                    }
                } catch (_: Exception) {}

                // 2. Yeni Güncelleme Kontrolü (Uygulama dışındayken de yeni sürüm çıktığında anında bildirim atar)
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

                    if (info.latestVersionCode > currentVersionCode) {
                        _latestUpdateFlow.value = info
                        if (prefs.isAutoUpdateEnabled.value &&
                            prefs.getLastNotifiedVersionCode() < info.latestVersionCode
                        ) {
                            prefs.setLastNotifiedVersionCode(info.latestVersionCode)
                            UpdateNotificationHelper.showUpdateNotification(
                                context = appContext,
                                versionName = info.latestVersionName,
                                changelog = info.changelog,
                                versionCode = info.latestVersionCode
                            )
                        }
                    } else {
                        _latestUpdateFlow.value = null
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
