package com.baverelci.accessgrab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.baverelci.accessgrab.accessibility.AccessibilityHelper
import com.baverelci.accessgrab.accessibility.ShakeDetectorHelper
import com.baverelci.accessgrab.downloader.ApkInstallHelper
import com.baverelci.accessgrab.notification.BackgroundSyncReceiver
import com.baverelci.accessgrab.notification.UpdateNotificationHelper
import com.baverelci.accessgrab.player.InAppPlayerManager
import com.baverelci.accessgrab.ui.screens.MainScreen
import com.baverelci.accessgrab.ui.theme.AccessGrabTheme
import com.baverelci.accessgrab.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var shakeDetector: ShakeDetectorHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Özel melodili bildirim kanallarını oluştur ve arka plan bildirim servisini zamanla
        UpdateNotificationHelper.createNotificationChannels(this)
        BackgroundSyncReceiver.schedulePeriodicSync(this)

        // Android 13+ (API 33+) cihazlarda durum çubuğu bildirim iznini kontrol et
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        shakeDetector = ShakeDetectorHelper(this) {
            if (viewModel.prefs.isShakeToPlayPauseEnabled.value && InAppPlayerManager.state.value.isActive) {
                val nowPlaying = InAppPlayerManager.togglePlayPause()
                if (viewModel.prefs.isHapticEnabled.value) {
                    AccessibilityHelper.vibrateSuccess(this)
                }
                val msg = if (nowPlaying) "Oynatmaya devam ediliyor" else "Oynatma duraklatıldı"
                window?.decorView?.let { AccessibilityHelper.announce(it, msg) }
            }
        }

        // Sosyal medya paylaşım menüsünden (Instagram, TikTok, YouTube, X vb.) gelen linki yakala
        handleIntent(intent)

        setContent {
            val highContrast by viewModel.prefs.isHighContrastLargeText.collectAsState()
            AccessGrabTheme(highContrastLargeText = highContrast) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        shakeDetector?.start()
        // Uygulama ön plana geldiğinde panoyu ve canlı duyuruyu kontrol et
        viewModel.checkClipboard(this)
        viewModel.checkAnnouncement()

        // Eğer kullanıcı ayarlar ekranından izin verip döndüyse ve indirilmiş güncelleme hazırsa doğrudan kur
        if (viewModel.isWaitingForInstallPermission && ApkInstallHelper.canInstallPackages(this)) {
            viewModel.isWaitingForInstallPermission = false
            val pendingApk = ApkInstallHelper.getDownloadedUpdateFile(this)
            if (pendingApk != null) {
                ApkInstallHelper.installApk(this, pendingApk)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stop()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val openTab = intent.getIntExtra("OPEN_TAB", -1)
        if (openTab != -1) {
            viewModel.selectTab(openTab)
        }
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            viewModel.handleSharedText(sharedText)
        }
    }
}
