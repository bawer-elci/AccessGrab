package com.baverelci.accessgrab.viewmodel

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baverelci.accessgrab.data.AppLogger
import com.baverelci.accessgrab.data.PreferencesManager
import com.baverelci.accessgrab.downloader.DownloadHelper
import com.baverelci.accessgrab.network.AccessGrabApi
import com.baverelci.accessgrab.network.AnnouncementInfo
import com.baverelci.accessgrab.network.ExtractRequest
import com.baverelci.accessgrab.network.ExtractResponse
import com.baverelci.accessgrab.network.FeedbackRequest
import com.baverelci.accessgrab.network.FormatOption
import com.baverelci.accessgrab.network.LogReportRequest
import com.baverelci.accessgrab.network.SearchRequest
import com.baverelci.accessgrab.network.SearchResultItem
import com.baverelci.accessgrab.network.UpdateInfo
import com.baverelci.accessgrab.notification.DownloadNotificationHelper
import com.baverelci.accessgrab.notification.UpdateNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

sealed class UiState {
    object Idle : UiState()
    data class Loading(val message: String = "Medya bilgisi alınıyor…") : UiState()
    data class SearchResultsReady(
        val query: String,
        val results: List<SearchResultItem>,
        val canLoadMore: Boolean = true,
        val isLoadingMore: Boolean = false
    ) : UiState()
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

data class QueuedDownloadItem(
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
        application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "2.0.0"
    } catch (_: Exception) {
        "2.0.0"
    }

    var isWaitingForInstallPermission = false

    private var lastDownloadParams: LastDownloadParams? = null
    private var activeDownloadJob: Job? = null
    private val pendingBatchUrls = ArrayDeque<String>()

    private val _downloadQueue = MutableStateFlow<List<QueuedDownloadItem>>(emptyList())
    val downloadQueue: StateFlow<List<QueuedDownloadItem>> = _downloadQueue.asStateFlow()

    private val _queueFeedbackMessage = MutableStateFlow<String?>(null)
    val queueFeedbackMessage: StateFlow<String?> = _queueFeedbackMessage.asStateFlow()

    fun clearQueueFeedback() {
        _queueFeedbackMessage.value = null
    }

    val prefs = PreferencesManager(application)

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _clipboardDetectedUrl = MutableStateFlow<String?>(null)
    val clipboardDetectedUrl: StateFlow<String?> = _clipboardDetectedUrl.asStateFlow()

    // Format ve Kalite Seçimleri
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

    private val _latestChangelogText = MutableStateFlow<String?>(null)
    val latestChangelogText: StateFlow<String?> = _latestChangelogText.asStateFlow()

    private val _updateErrorMessage = MutableStateFlow<String?>(null)
    val updateErrorMessage: StateFlow<String?> = _updateErrorMessage.asStateFlow()

    // Tanılama Kayıtları (Log Gönderme) Durumları
    private val _isSendingLogs = MutableStateFlow(false)
    val isSendingLogs: StateFlow<Boolean> = _isSendingLogs.asStateFlow()

    private val _logReportStatusMessage = MutableStateFlow<String?>(null)
    val logReportStatusMessage: StateFlow<String?> = _logReportStatusMessage.asStateFlow()

    // Canlı Duyuru / Mesaj Sistemi (#43)
    private val _activeAnnouncement = MutableStateFlow<AnnouncementInfo?>(null)
    val activeAnnouncement: StateFlow<AnnouncementInfo?> = _activeAnnouncement.asStateFlow()

    // Geliştiriciye Öneri / Mesaj Gönderme (#38)
    private val _isSendingFeedback = MutableStateFlow(false)
    val isSendingFeedback: StateFlow<Boolean> = _isSendingFeedback.asStateFlow()

    private val _feedbackStatusMessage = MutableStateFlow<String?>(null)
    val feedbackStatusMessage: StateFlow<String?> = _feedbackStatusMessage.asStateFlow()

    fun clearFeedbackStatus() {
        _feedbackStatusMessage.value = null
    }

    init {
        AppLogger.init(application, currentVersionName)
        checkForUpdates(isManual = false)
        checkAnnouncement()
    }

    fun checkAnnouncement() {
        viewModelScope.launch {
            try {
                val info = AccessGrabApi.service.getAnnouncement()
                if (info.active && info.id.isNotBlank() && info.message.isNotBlank()) {
                    if (info.id != prefs.getLastDismissedAnnouncementId()) {
                        _activeAnnouncement.value = info
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun dismissAnnouncement() {
        _activeAnnouncement.value?.let { info ->
            prefs.setLastDismissedAnnouncementId(info.id)
        }
        _activeAnnouncement.value = null
    }

    fun sendUserFeedback(senderName: String, message: String, onSuccess: () -> Unit = {}) {
        val cleanMsg = message.trim()
        if (cleanMsg.length < 3) {
            _feedbackStatusMessage.value = "Lütfen en az birkaç kelimelik mesaj veya önerinizi yazın."
            return
        }
        if (_isSendingFeedback.value) return
        _isSendingFeedback.value = true
        _feedbackStatusMessage.value = "Mesajınız gönderiliyor…"

        viewModelScope.launch {
            try {
                val req = FeedbackRequest(
                    senderName = senderName.trim().ifBlank { "Anonim Kullanıcı" },
                    message = cleanMsg,
                    deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE ?: "Unknown"})",
                    appVersion = currentVersionName
                )
                val res = AccessGrabApi.service.sendFeedback(req)
                _isSendingFeedback.value = false
                _feedbackStatusMessage.value = res.message ?: "Mesajınız ve öneriniz geliştiriciye başarıyla iletildi. Teşekkür ederiz!"
                onSuccess()
            } catch (e: Exception) {
                _isSendingFeedback.value = false
                _feedbackStatusMessage.value = "Mesaj gönderilemedi: ${e.localizedMessage ?: "Bağlantınızı kontrol edin."}"
            }
        }
    }

    fun shareApp(context: Context) {
        try {
            val shareText = buildString {
                appendLine("AccessGrab — Görme engelliler ve TalkBack kullanıcıları için %100 erişilebilir, reklamsız sosyal medya video ve MP3 ses indiricisi!")
                appendLine()
                appendLine("YouTube, Instagram, TikTok, X (Twitter) ve Facebook üzerinden kelimeyle arama yapabilir, indirmeden önce canlı dinleyebilir, zil/alarm sesi yapabilir ve dahili oynatıcıda dinleyebilirsiniz.")
                appendLine()
                append("Doğrudan APK İndirme Bağlantısı: https://github.com/bawer-elci/AccessGrab/releases/latest/download/AccessGrab.apk")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "AccessGrab - Erişilebilir Sosyal Medya İndiricisi")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(intent, "AccessGrab Uygulamasını Arkadaşına Paylaş").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
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

        if (rawUrl.isBlank()) {
            _uiState.value = UiState.Error("Lütfen aramak istediğiniz kelimeyi veya bir bağlantı adresi girin.")
            return
        }

        val allUrls = extractAllUrlsFromText(rawUrl)

        // Eğer girilen metin bir URL değilse (http/https içermiyorsa) doğrudan kelimeyle arama yap
        if (allUrls.isEmpty() && !rawUrl.startsWith("http://", ignoreCase = true) && !rawUrl.startsWith("https://", ignoreCase = true)) {
            searchMediaByQuery(rawUrl)
            return
        }

        val cleanUrl = allUrls.firstOrNull() ?: rawUrl

        // Kullanıcı birden fazla linki aynı anda yapıştırdıysa kalanları toplu indirme kuyruğuna hazırla
        pendingBatchUrls.clear()
        if (allUrls.size > 1) {
            allUrls.drop(1).forEach { pendingBatchUrls.addLast(it) }
            _queueFeedbackMessage.value = "${allUrls.size} adet bağlantı algılandı. İlk medya inceleniyor, diğer ${allUrls.size - 1} bağlantı sıraya alınacak."
        }

        _uiState.value = UiState.Loading("Medya analiz ediliyor...")
        AppLogger.log("EXTRACT_START", "Bağlantı inceleniyor: $cleanUrl (Toplam algılanan: ${allUrls.size})")

        viewModelScope.launch {
            try {
                val response = AccessGrabApi.service.extractMedia(ExtractRequest(url = cleanUrl))
                AppLogger.log("EXTRACT_OK", "Medya bulundu: [${response.platform}] ${response.title} (${response.durationFormatted})")
                _uiState.value = UiState.MediaReady(response)

                // Otomatik hızlı indirme kontrolü
                val defFormat = prefs.defaultFormat.value
                if (defFormat == "mp4" || defFormat == "mp3") {
                    val app = getApplication<Application>()
                    startTrackedDownload(app, response.url, defFormat, response.title)
                }
            } catch (e: Exception) {
                android.util.Log.e("AccessGrab", "Extract error: ${e.message}", e)
                AppLogger.log("EXTRACT_ERR", "Medya analiz hatası ($cleanUrl)", e)
                val errorMsg = e.localizedMessage ?: "Sunucuya bağlanılamadı. Lütfen tekrar deneyin."
                _uiState.value = UiState.Error("Hata: $errorMsg", canRetry = true)
            }
        }
    }

    private fun searchMediaByQuery(query: String) {
        _uiState.value = UiState.Loading("\"$query\" aranıyor…")
        AppLogger.log("SEARCH_START", "Kelime araması başlatıldı: $query")

        viewModelScope.launch {
            try {
                val response = AccessGrabApi.service.searchMedia(SearchRequest(query = query, limit = 8))
                if (response.results.isEmpty()) {
                    _uiState.value = UiState.Error("\"$query\" için sonuç bulunamadı. Lütfen farklı bir kelime deneyin.")
                } else {
                    AppLogger.log("SEARCH_OK", "${response.results.size} sonuç bulundu ($query)")
                    _uiState.value = UiState.SearchResultsReady(
                        query = query,
                        results = response.results,
                        canLoadMore = response.results.size >= 8,
                        isLoadingMore = false
                    )
                }
            } catch (e: Exception) {
                AppLogger.log("SEARCH_ERR", "Arama hatası ($query)", e)
                val errorMsg = e.localizedMessage ?: "Arama sunucusuna bağlanılamadı."
                _uiState.value = UiState.Error("Arama hatası: $errorMsg", canRetry = true)
            }
        }
    }

    /**
     * Arama sonuçlarında "Daha Fazla Sonuç Getir" (#17): 20 sonuca kadar genişletir.
     */
    fun loadMoreSearchResults() {
        val currentState = _uiState.value as? UiState.SearchResultsReady ?: return
        if (currentState.isLoadingMore || !currentState.canLoadMore) return

        _uiState.value = currentState.copy(isLoadingMore = true)
        AppLogger.log("SEARCH_MORE", "Daha fazla sonuç getiriliyor: ${currentState.query}")

        viewModelScope.launch {
            try {
                val response = AccessGrabApi.service.searchMedia(SearchRequest(query = currentState.query, limit = 20))
                val mergedResults = if (response.results.isNotEmpty()) response.results else currentState.results
                _uiState.value = UiState.SearchResultsReady(
                    query = currentState.query,
                    results = mergedResults,
                    canLoadMore = false,
                    isLoadingMore = false
                )
                _queueFeedbackMessage.value = "\"${currentState.query}\" için toplam ${mergedResults.size} arama sonucu listelendi."
            } catch (e: Exception) {
                AppLogger.log("SEARCH_MORE_ERR", "Daha fazla sonuç getirilemedi (${currentState.query})", e)
                _uiState.value = currentState.copy(isLoadingMore = false)
                _queueFeedbackMessage.value = "Daha fazla sonuç yüklenemedi."
            }
        }
    }

    fun selectSearchResultForQuality(item: SearchResultItem) {
        _uiState.value = UiState.MediaReady(
            ExtractResponse(
                success = true,
                platform = item.platform,
                title = item.title,
                author = item.author,
                duration = item.duration,
                durationFormatted = item.durationFormatted,
                thumbnail = item.thumbnail,
                url = item.url,
                supportedFormats = listOf(
                    FormatOption("mp4", "Video (MP4)", "HD Video"),
                    FormatOption("mp3", "Ses (MP3)", "Yüksek Kalite Ses")
                )
            )
        )
    }

    fun startTrackedDownload(
        context: Context,
        mediaUrl: String,
        format: String,
        title: String,
        quality: String? = null
    ) {
        val appContext = getApplication<Application>().applicationContext

        // Eğer kullanıcı tek seferde birden fazla URL yapıştırdıysa kalan URL'leri arka planda çözüp kuyruğa ekle
        if (pendingBatchUrls.isNotEmpty()) {
            val batchUrls = pendingBatchUrls.toList()
            pendingBatchUrls.clear()
            viewModelScope.launch {
                for (extraUrl in batchUrls) {
                    try {
                        val info = AccessGrabApi.service.extractMedia(ExtractRequest(url = extraUrl))
                        val newItem = QueuedDownloadItem(
                            mediaUrl = info.url,
                            format = format,
                            title = info.title,
                            quality = quality
                        )
                        _downloadQueue.value = _downloadQueue.value + newItem
                        AppLogger.log("QUEUE_ADD", "Toplu kuyruğa eklendi: ${info.title}")
                    } catch (e: Exception) {
                        AppLogger.log("QUEUE_ERR", "Kuyruk URL çözümlenemedi: $extraUrl", e)
                    }
                }
            }
        }

        // Eğer halihazırda aktif bir indirme sürüyorsa yeni isteği kuyruğa ekle
        if (activeDownloadJob?.isActive == true) {
            val queuedItem = QueuedDownloadItem(mediaUrl, format, title, quality)
            _downloadQueue.value = _downloadQueue.value + queuedItem
            _queueFeedbackMessage.value = "Kuyruğa eklendi: $title. Sırada ${_downloadQueue.value.size} medya bekliyor."
            AppLogger.log("QUEUE_ADD", "Aktif indirme sırasında kuyruğa eklendi: $title")
            return
        }

        executeDownloadItem(appContext, QueuedDownloadItem(mediaUrl, format, title, quality))
    }

    private fun executeDownloadItem(appContext: Context, item: QueuedDownloadItem) {
        val mediaUrl = item.mediaUrl
        val format = item.format
        val title = item.title
        val quality = item.quality

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

        DownloadNotificationHelper.showProgressNotification(
            context = appContext,
            title = title,
            format = formatLabel,
            progress = 0,
            isIndeterminate = true,
            queueRemaining = _downloadQueue.value.size
        )

        AppLogger.log("DOWNLOAD_START", "İndirme başlatıldı: [$formatLabel] $title")

        activeDownloadJob = viewModelScope.launch {
            try {
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
                        DownloadNotificationHelper.showProgressNotification(
                            context = appContext,
                            title = title,
                            format = formatLabel,
                            progress = progress,
                            isIndeterminate = total <= 0L && downloaded <= 0L,
                            queueRemaining = _downloadQueue.value.size
                        )
                    }
                )

                if (result.isSuccess) {
                    val savedFile = result.getOrNull()
                    AppLogger.log("DOWNLOAD_OK", "İndirme tamamlandı: ${savedFile?.name} (${savedFile?.length()} bayt)")
                    DownloadNotificationHelper.showCompletedNotification(
                        context = appContext,
                        title = title,
                        format = formatLabel
                    )

                    // Kuyrukta bekleyen başka medya varsa otomatik olarak sıradakine geç
                    val currentQueue = _downloadQueue.value
                    if (currentQueue.isNotEmpty()) {
                        val nextItem = currentQueue.first()
                        _downloadQueue.value = currentQueue.drop(1)
                        _queueFeedbackMessage.value = "$title tamamlandı. Sıradaki indiriliyor: ${nextItem.title}"
                        executeDownloadItem(appContext, nextItem)
                    } else {
                        _uiState.value = UiState.DownloadCompleted(
                            title = title,
                            format = formatLabel
                        )
                    }
                } else {
                    DownloadNotificationHelper.cancelProgressNotification(appContext)
                    val ex = result.exceptionOrNull()
                    AppLogger.log("DOWNLOAD_ERR", "İndirme başarısız: [$formatLabel] $title", ex)
                    val err = ex?.localizedMessage
                        ?: "İndirme işlemi tamamlanamadı. Bağlantınızı kontrol edip yeniden deneyebilirsiniz."
                    _uiState.value = UiState.Error(
                        message = err,
                        canRetry = true
                    )
                }
            } catch (_: CancellationException) {
                DownloadNotificationHelper.cancelProgressNotification(appContext)
                AppLogger.log("DOWNLOAD_CANCEL", "İndirme kullanıcı tarafından iptal edildi: $title")
            }
        }
    }

    /**
     * Aktif indirmeyi iptal eder. Kuyrukta bekleyen başka medya varsa sıradakine geçer.
     */
    fun cancelActiveDownload() {
        val appContext = getApplication<Application>().applicationContext
        DownloadHelper.cancelCurrentCall()
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        DownloadNotificationHelper.cancelProgressNotification(appContext)

        val currentQueue = _downloadQueue.value
        if (currentQueue.isNotEmpty()) {
            val nextItem = currentQueue.first()
            _downloadQueue.value = currentQueue.drop(1)
            _queueFeedbackMessage.value = "Mevcut indirme iptal edildi. Sıradakine geçiliyor: ${nextItem.title}"
            executeDownloadItem(appContext, nextItem)
        } else {
            _uiState.value = UiState.Idle
            _queueFeedbackMessage.value = "İndirme iptal edildi."
        }
    }

    /**
     * Hem aktif indirmeyi hem de kuyrukta bekleyen tüm medyaları iptal eder.
     */
    fun clearQueueAndCancelAll() {
        val appContext = getApplication<Application>().applicationContext
        pendingBatchUrls.clear()
        _downloadQueue.value = emptyList()
        DownloadHelper.cancelCurrentCall()
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        DownloadNotificationHelper.cancelProgressNotification(appContext)
        _uiState.value = UiState.Idle
        _queueFeedbackMessage.value = "Aktif indirme ve tüm kuyruk iptal edildi."
    }

    /**
     * Akıllı Yeniden Deneme (Smart Retry)
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
            _updateInfo.value = null
            _isUpToDateDialogVisible.value = false
            _updateStatusMessage.value = "Güncellemeler denetleniyor…"
            _updateErrorMessage.value = null
        }

        viewModelScope.launch {
            try {
                val info = AccessGrabApi.service.getLatestVersion()
                _isCheckingUpdate.value = false
                _latestChangelogText.value = info.changelog

                if (info.latestVersionCode > currentVersionCode) {
                    _updateInfo.value = info
                    _updateStatusMessage.value = "Yeni sürüm mevcut: v${info.latestVersionName}. Güncelleme özellikleri: ${info.changelog}"

                    // Hoş melodili bildirim gönder
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
                    _updateStatusMessage.value = "Uygulamanız güncel: Sürüm v${currentVersionName}. Bu sürümün özellikleri: ${info.changelog}"
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

    /**
     * Cihaz ve uygulama hata/tanılama kayıtlarını geliştirici sunucusuna gönderir.
     */
    fun sendDiagnosticLogs(userNote: String? = null) {
        if (_isSendingLogs.value) return
        _isSendingLogs.value = true
        _logReportStatusMessage.value = "Tanılama kayıtları gönderiliyor…"

        viewModelScope.launch {
            try {
                AppLogger.log("LOG_REPORT", "Kullanıcı tanılama kayıtlarını gönderiyor")
                val reportText = withContext(Dispatchers.IO) {
                    AppLogger.buildFullReport()
                }
                val request = LogReportRequest(
                    deviceManufacturer = Build.MANUFACTURER ?: "Unknown",
                    deviceModel = Build.MODEL ?: "Unknown",
                    androidVersion = Build.VERSION.RELEASE ?: "Unknown",
                    sdkInt = Build.VERSION.SDK_INT,
                    appVersion = currentVersionName,
                    logs = reportText,
                    userNote = userNote
                )
                val response = AccessGrabApi.service.reportClientLog(request)
                _isSendingLogs.value = false
                _logReportStatusMessage.value = response.message
                    ?: "Hata ve tanılama kayıtları başarıyla gönderildi (Kod: ${response.reportId ?: "OK"})."
            } catch (e: Exception) {
                _isSendingLogs.value = false
                _logReportStatusMessage.value = "Kayıtlar gönderilemedi: ${e.localizedMessage ?: "Bağlantınızı kontrol edin."}"
            }
        }
    }

    private fun extractUrlFromText(text: String): String? {
        return extractAllUrlsFromText(text).firstOrNull()
    }

    private fun extractAllUrlsFromText(text: String): List<String> {
        val urlRegex = "https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+"
        val pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        val list = mutableListOf<String>()
        while (matcher.find()) {
            matcher.group(0)?.let { found ->
                if (!list.contains(found)) {
                    list.add(found)
                }
            }
        }
        return list
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
