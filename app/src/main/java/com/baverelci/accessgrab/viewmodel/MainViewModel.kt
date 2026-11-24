package com.baverelci.accessgrab.viewmodel

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.DownloadManager
import com.baverelci.accessgrab.data.PreferencesManager
import com.baverelci.accessgrab.downloader.DownloadHelper
import com.baverelci.accessgrab.network.AccessGrabApi
import com.baverelci.accessgrab.network.ExtractRequest
import com.baverelci.accessgrab.network.ExtractResponse
import com.baverelci.accessgrab.network.UpdateInfo
import com.baverelci.accessgrab.notification.UpdateNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

sealed class UiState {
    object Idle : UiState()
    data class Loading(val message: String = "Medya bilgisi alınıyor…") : UiState()
    data class MediaReady(val data: ExtractResponse) : UiState()
    data class Downloading(
        val title: String,
        val format: String,
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val downloadId: Long
    ) : UiState()
    data class DownloadCompleted(
        val title: String,
        val format: String
    ) : UiState()
    data class DownloadInitiated(val message: String, val format: String) : UiState()
    data class Error(val message: String, val canRetry: Boolean = false) : UiState()
}

data class LastDownloadParams(
    val mediaUrl: String,
    val format: String,
    val title: String,
    val quality: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val currentVersionCode: Long = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            application.packageManager.getPackageInfo(application.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            application.packageManager.getPackageInfo(application.packageName, 0).versionCode.toLong()
        }
    } catch (_: Exception) {
        1L
    }

    val currentVersionName: String = try {
        application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "1.2.0"
    } catch (_: Exception) {
        "1.2.0"
    }

    var isWaitingForInstallPermission = false

    private var lastDownloadParams: LastDownloadParams? = null

    val prefs = PreferencesManager(application)

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _clipboardDetectedUrl = MutableStateFlow<String?>(null)
    val clipboardDetectedUrl: StateFlow<String?> = _clipboardDetectedUrl.asStateFlow()

    // Format ve Kalite Seçimleri (v1.4.0)
    private val _selectedFormat = MutableStateFlow("mp4")
    val selectedFormat: StateFlow<String> = _selectedFormat.asStateFlow()

    private val _selectedQuality = MutableStateFlow("720")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    fun selectFormat(format: String) {
        _selectedFormat.value = format
        if (format.lowercase() == "mp3") {
            _selectedQuality.value = "192"
        } else {
            _selectedQuality.value = "720"
        }
    }

    fun selectQuality(quality: String) {
        _selectedQuality.value = quality
    }

    // Güncelleme Durumları
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateStatusMessage = MutableStateFlow<String?>(null)
    val updateStatusMessage: StateFlow<String?> = _updateStatusMessage.asStateFlow()

    private val _isUpToDateDialogVisible = MutableStateFlow(false)
    val isUpToDateDialogVisible: StateFlow<Boolean> = _isUpToDateDialogVisible.asStateFlow()

    private val _updateErrorMessage = MutableStateFlow<String?>(null)
    val updateErrorMessage: StateFlow<String?> = _updateErrorMessage.asStateFlow()

    init {
        // Uygulama açılışında arka planda sessizce güncelleme kontrolü
        checkForUpdates(isManual = false)
    }

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
        if (_uiState.value !is UiState.Idle && _uiState.value !is UiState.Loading) {
            _uiState.value = UiState.Idle
        }
    }

    fun clearInput() {
        _urlInput.value = ""
        _uiState.value = UiState.Idle
    }

    fun handleSharedText(sharedText: String?) {
        if (sharedText.isNullOrBlank()) return
        val extractedUrl = extractUrlFromText(sharedText)
        if (extractedUrl != null) {
            _urlInput.value = extractedUrl
            _currentTab.value = 0 // İndirme sekmesine geç
            fetchMediaInfo(extractedUrl)
        } else {
            _uiState.value = UiState.Error("Paylaşılan metin içinde geçerli bir bağlantı bulunamadı.")
        }
    }

    fun checkClipboard(context: Context) {
        if (!prefs.isClipboardEnabled.value) return
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            if (clipboard.hasPrimaryClip() && (clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true)) {
                val item = clipboard.primaryClip?.getItemAt(0)
                val text = item?.text?.toString() ?: ""
                val foundUrl = extractUrlFromText(text)
                if (foundUrl != null && isSupportedPlatform(foundUrl) && foundUrl != _urlInput.value) {
                    _clipboardDetectedUrl.value = foundUrl
                }
            }
        } catch (_: Exception) {}
    }

    fun applyDetectedClipboardUrl() {
        val url = _clipboardDetectedUrl.value ?: return
        _urlInput.value = url
        _clipboardDetectedUrl.value = null
        fetchMediaInfo(url)
    }

    fun dismissClipboardPrompt() {
        _clipboardDetectedUrl.value = null
    }

    fun fetchMediaInfo(targetUrl: String? = null) {
        val rawUrl = (targetUrl ?: _urlInput.value).trim()
        val cleanUrl = extractUrlFromText(rawUrl) ?: rawUrl

        if (cleanUrl.isBlank()) {
            _uiState.value = UiState.Error("Lütfen bir bağlantı adresi girin.")
            return
        }

        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            _uiState.value = UiState.Error("Geçerli bir internet bağlantısı girin (http veya https ile başlamalı).")
            return
        }

        _uiState.value = UiState.Loading("Medya analiz ediliyor...")

        viewModelScope.launch {
            try {
                val response = AccessGrabApi.service.extractMedia(ExtractRequest(url = cleanUrl))
                _uiState.value = UiState.MediaReady(response)

                // Otomatik hızlı indirme kontrolü
                val defFormat = prefs.defaultFormat.value
                if (defFormat == "mp4" || defFormat == "mp3") {
                    val app = getApplication<Application>()
                    startTrackedDownload(app, response.url, defFormat, response.title)
                }
            } catch (e: Exception) {
                android.util.Log.e("AccessGrab", "Extract error: ${e.message}", e)
                val errorMsg = e.localizedMessage ?: "Sunucuya bağlanılamadı. Lütfen tekrar deneyin."
                _uiState.value = UiState.Error("Hata: $errorMsg", canRetry = true)
            }
        }
    }

    fun startTrackedDownload(
        context: Context,
        mediaUrl: String,
        format: String,
        title: String,
        quality: String? = null
    ) {
        lastDownloadParams = LastDownloadParams(mediaUrl, format, title, quality)
        val formatLabel = if (!quality.isNullOrBlank()) "${format.uppercase()} ($quality)" else format.uppercase()

        _uiState.value = UiState.Downloading(
            title = title,
            format = formatLabel,
            progress = 0,
            downloadedBytes = 0L,
            totalBytes = 0L,
            downloadId = 0L
        )

        val appContext = getApplication<Application>().applicationContext

        viewModelScope.launch {
            val result = DownloadHelper.downloadMediaDirect(
                context = appContext,
                mediaUrl = mediaUrl,
                format = format,
                title = title,
                quality = quality,
                onProgress = { progress, downloaded, total ->
                    _uiState.value = UiState.Downloading(
                        title = title,
                        format = formatLabel,
                        progress = progress,
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        downloadId = 0L
                    )
                }
            )

            if (result.isSuccess) {
                _uiState.value = UiState.DownloadCompleted(
                    title = title,
                    format = formatLabel
                )
            } else {
                val err = result.exceptionOrNull()?.localizedMessage
                    ?: "İndirme işlemi tamamlanamadı. Bağlantınızı kontrol edip yeniden deneyebilirsiniz."
                _uiState.value = UiState.Error(
                    message = err,
                    canRetry = true
                )
            }
        }
    }

    /**
     * Akıllı Yeniden Deneme (Smart Retry):
     * Hata alan son indirmeyi veya son medya getirme isteğini aynı parametrelerle tekrar tetikler.
     */
    fun retryLastAction(context: Context) {
        val params = lastDownloadParams
        if (params != null) {
            startTrackedDownload(
                context = context,
                mediaUrl = params.mediaUrl,
                format = params.format,
                title = params.title,
                quality = params.quality
            )
        } else if (_urlInput.value.isNotBlank()) {
            fetchMediaInfo(_urlInput.value)
        }
    }

    fun onDownloadTriggered(format: String, title: String) {
        _uiState.value = UiState.DownloadInitiated(
            message = "$title ($format) indirilmeye başlandı.",
            format = format
        )
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    /**
     * Uygulama içi güncelleme kontrolü
     */
    fun checkForUpdates(isManual: Boolean) {
        if (!isManual && !prefs.isAutoUpdateEnabled.value) return

        _isCheckingUpdate.value = true
        if (isManual) {
            _updateStatusMessage.value = "Güncellemeler denetleniyor…"
            _updateErrorMessage.value = null
        }

        viewModelScope.launch {
            try {
                val info = AccessGrabApi.service.getLatestVersion()
                _isCheckingUpdate.value = false

                if (info.latestVersionCode > currentVersionCode) {
                    _updateInfo.value = info
                    _updateStatusMessage.value = "Yeni bir sürüm mevcut: v${info.latestVersionName}"

                    // Bildirim gönder
                    val app = getApplication<Application>()
                    if (prefs.isAutoUpdateEnabled.value && prefs.getLastNotifiedVersionCode() < info.latestVersionCode) {
                        UpdateNotificationHelper.showUpdateNotification(
                            app,
                            info.latestVersionName,
                            info.changelog
                        )
                        prefs.setLastNotifiedVersionCode(info.latestVersionCode)
                    }
                } else {
                    _updateInfo.value = null
                    _updateStatusMessage.value = "Şu anda yeni bir sürüm mevcut değil."
                    if (isManual) {
                        _isUpToDateDialogVisible.value = true
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AccessGrab", "Update check error: ${e.message}", e)
                _isCheckingUpdate.value = false
                if (isManual) {
                    val err = "Güncelleme sunucusuna bağlanılamadı: ${e.localizedMessage ?: "Lütfen internet bağlantınızı kontrol edin."}"
                    _updateStatusMessage.value = err
                    _updateErrorMessage.value = err
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    fun dismissUpToDateDialog() {
        _isUpToDateDialogVisible.value = false
    }

    fun dismissUpdateErrorDialog() {
        _updateErrorMessage.value = null
    }

    private fun extractUrlFromText(text: String): String? {
        val urlRegex = "https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+"
        val pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(0) else null
    }

    private fun isSupportedPlatform(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("instagram.com") ||
                u.contains("tiktok.com") ||
                u.contains("youtube.com") ||
                u.contains("youtu.be") ||
                u.contains("twitter.com") ||
                u.contains("x.com") ||
                u.contains("facebook.com") ||
                u.contains("fb.watch")
    }
}
