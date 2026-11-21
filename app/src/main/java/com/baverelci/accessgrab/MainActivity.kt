package com.baverelci.accessgrab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.baverelci.accessgrab.downloader.ApkInstallHelper
import com.baverelci.accessgrab.ui.screens.MainScreen
import com.baverelci.accessgrab.ui.theme.AccessGrabTheme
import com.baverelci.accessgrab.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sosyal medya paylaşım menüsünden (Instagram, TikTok, YouTube vb.) gelen linki yakala
        handleIntent(intent)

        setContent {
            AccessGrabTheme {
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
        // Uygulama ön plana geldiğinde panoyu kontrol et
        viewModel.checkClipboard(this)

        // Eğer kullanıcı ayarlar ekranından izin verip döndüyse ve indirilmiş güncelleme hazırsa doğrudan kur
        if (viewModel.isWaitingForInstallPermission && ApkInstallHelper.canInstallPackages(this)) {
            viewModel.isWaitingForInstallPermission = false
            val pendingApk = ApkInstallHelper.getDownloadedUpdateFile(this)
            if (pendingApk != null) {
                ApkInstallHelper.installApk(this, pendingApk)
            }
        }
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
