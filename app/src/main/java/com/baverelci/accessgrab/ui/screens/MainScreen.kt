package com.baverelci.accessgrab.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.R
import com.baverelci.accessgrab.accessibility.AccessibilityHelper
import com.baverelci.accessgrab.ui.components.AccessibleButton
import com.baverelci.accessgrab.ui.components.ClipboardPromptCard
import com.baverelci.accessgrab.ui.components.DownloadCompletedCard
import com.baverelci.accessgrab.ui.components.DownloadErrorCard
import com.baverelci.accessgrab.ui.components.DownloadProgressCard
import com.baverelci.accessgrab.ui.components.QualitySelectorCard
import com.baverelci.accessgrab.ui.components.SearchResultsCard
import com.baverelci.accessgrab.ui.components.UrlInputCard
import com.baverelci.accessgrab.viewmodel.MainViewModel
import com.baverelci.accessgrab.viewmodel.UiState

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentTab by viewModel.currentTab.collectAsState()
    val isHaptic by viewModel.prefs.isHapticEnabled.collectAsState()
    val isCompletionSound by viewModel.prefs.isCompletionSoundEnabled.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val queueFeedback by viewModel.queueFeedbackMessage.collectAsState()

    LaunchedEffect(queueFeedback) {
        queueFeedback?.let { msg ->
            AccessibilityHelper.announce(view, msg)
            viewModel.clearQueueFeedback()
        }
    }

    // Sekme değiştiğinde veya medya analizi/indirme başladığında klavyeyi otomatik kapat
    LaunchedEffect(currentTab, uiState) {
        if (currentTab != 0 || uiState !is UiState.Idle) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // TalkBack canlı durum bildirimleri
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Loading -> {
                AccessibilityHelper.announce(view, state.message)
            }
            is UiState.SearchResultsReady -> {
                if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                AccessibilityHelper.announce(view, "\"${state.query}\" için ${state.results.size} sonuç bulundu.")
            }
            is UiState.MediaReady -> {
                if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                val msg = "${state.data.platform} medyası hazır: ${state.data.title}. MP4 video veya MP3 ses olarak indirebilirsiniz."
                AccessibilityHelper.announce(view, msg)
            }
            is UiState.Downloading -> {
                if (state.progress == 0 || state.progress == 50) {
                    AccessibilityHelper.announce(view, "%${state.progress} indirildi")
                }
            }
            is UiState.DownloadCompleted -> {
                if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                if (isCompletionSound) AccessibilityHelper.playCompletionSound()
                AccessibilityHelper.announce(view, "İndirme tamamlandı: ${state.title}")
            }
            is UiState.DownloadInitiated -> {
                if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                AccessibilityHelper.announce(view, state.message)
            }
            is UiState.Error -> {
                if (isHaptic) AccessibilityHelper.vibrateError(context)
                AccessibilityHelper.announce(view, state.message)
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.semantics {
                    this.contentDescription = "Ana Gezinme Çubuğu"
                }
            ) {
                // 1. İndir Sekmesi
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    label = { Text("İndir") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.semantics {
                        this.contentDescription = "İndir sekmesi, ${if (currentTab == 0) "seçili" else "seçili değil"}"
                    }
                )

                // 2. Kütüphanem Sekmesi
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    label = { Text("Kütüphane") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.semantics {
                        this.contentDescription = "Kütüphane sekmesi, ${if (currentTab == 1) "seçili" else "seçili değil"}"
                    }
                )

                // 3. Ayarlar Sekmesi
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ayarlar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.semantics {
                        this.contentDescription = "Ayarlar sekmesi, ${if (currentTab == 2) "seçili" else "seçili değil"}"
                    }
                )

                // 4. Hakkında Sekmesi
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Hakkında") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.semantics {
                        this.contentDescription = "Hakkında sekmesi, ${if (currentTab == 3) "seçili" else "seçili değil"}"
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentTab) {
            0 -> DownloaderContent(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> LibraryScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            2 -> SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            3 -> AboutScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun DownloaderContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHaptic by viewModel.prefs.isHapticEnabled.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val clipboardUrl by viewModel.clipboardDetectedUrl.collectAsState()
    val downloadQueue by viewModel.downloadQueue.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Başlık ve Logo Alanı
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "AccessGrab uygulama logosu",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column {
                Text(
                    text = "AccessGrab",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Erişilebilir Sosyal Medya İndiricisi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Panoda Algılanan Bağlantı Bildirimi (Varsa)
        if (clipboardUrl != null) {
            ClipboardPromptCard(
                url = clipboardUrl ?: "",
                onUseUrl = { viewModel.applyDetectedClipboardUrl() },
                onDismiss = { viewModel.dismissClipboardPrompt() }
            )
        }

        // URL Giriş ve Medyayı İncele Kartı
        UrlInputCard(
            urlInput = urlInput,
            onUrlChanged = { viewModel.onUrlChanged(it) },
            onClearInput = { viewModel.clearInput() },
            onFetchMedia = { viewModel.fetchMediaInfo() },
            onPasteAndFetch = { pasted ->
                viewModel.onUrlChanged(pasted)
                viewModel.fetchMediaInfo(pasted)
            },
            isLoading = uiState is UiState.Loading
        )

        // Yükleniyor Göstergesi
        if (uiState is UiState.Loading) {
            val loadingMsg = (uiState as UiState.Loading).message
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        this.liveRegion = LiveRegionMode.Polite
                        this.contentDescription = loadingMsg
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = loadingMsg,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Hata Alanı (Akıllı Yeniden Deneme Butonlu)
        if (uiState is UiState.Error) {
            val errorState = uiState as UiState.Error
            DownloadErrorCard(
                errorMessage = errorState.message,
                canRetry = errorState.canRetry,
                onRetry = { viewModel.retryLastAction(context) },
                onDismiss = { viewModel.resetState() }
            )
        }

        // Doğrudan Kelimeyle Arama Sonuçları Listesi
        if (uiState is UiState.SearchResultsReady) {
            val searchState = uiState as UiState.SearchResultsReady
            SearchResultsCard(
                query = searchState.query,
                results = searchState.results,
                onSelectResultForQuality = { item ->
                    viewModel.selectSearchResultForQuality(item)
                },
                onDirectDownload = { item, fmt, qual ->
                    viewModel.selectFormat(fmt)
                    viewModel.selectQuality(qual)
                    viewModel.startTrackedDownload(
                        context = context,
                        mediaUrl = item.url,
                        format = fmt,
                        title = item.title,
                        quality = qual
                    )
                }
            )
        }

        // Medya Başarıyla Yüklendiğinde Format ve Kalite Seçimi
        if (uiState is UiState.MediaReady) {
            val media = (uiState as UiState.MediaReady).data
            QualitySelectorCard(
                media = media,
                selectedFormat = selectedFormat,
                selectedQuality = selectedQuality,
                onFormatSelected = { viewModel.selectFormat(it) },
                onQualitySelected = { viewModel.selectQuality(it) },
                onStartDownload = {
                    viewModel.startTrackedDownload(
                        context = context,
                        mediaUrl = media.url,
                        format = selectedFormat,
                        title = media.title,
                        quality = selectedQuality
                    )
                },
                onQuickDownload = { fmt, qual ->
                    viewModel.selectFormat(fmt)
                    viewModel.selectQuality(qual)
                    viewModel.startTrackedDownload(
                        context = context,
                        mediaUrl = media.url,
                        format = fmt,
                        title = media.title,
                        quality = qual
                    )
                }
            )
        }

        // Canlı İlerleme Çubuğu ve İndirme Yüzdesi Göstergesi
        if (uiState is UiState.Downloading) {
            val state = uiState as UiState.Downloading
            DownloadProgressCard(
                title = state.title,
                format = state.format,
                progress = state.progress,
                downloadedBytes = state.downloadedBytes,
                totalBytes = state.totalBytes,
                queueCount = downloadQueue.size,
                queuedTitles = downloadQueue.map { it.title },
                onCancelDownload = {
                    if (isHaptic) AccessibilityHelper.vibrateError(context)
                    viewModel.cancelActiveDownload()
                },
                onCancelAllQueue = {
                    if (isHaptic) AccessibilityHelper.vibrateError(context)
                    viewModel.clearQueueAndCancelAll()
                }
            )
        }

        // İndirme Tamamlandı Kartı
        if (uiState is UiState.DownloadCompleted) {
            val state = uiState as UiState.DownloadCompleted
            DownloadCompletedCard(
                title = state.title,
                format = state.format,
                onOpenFile = {
                    viewModel.selectTab(1)
                },
                onNewMedia = {
                    viewModel.resetState()
                    viewModel.clearInput()
                }
            )
        }

        // İndirme Başlatıldı Bildirimi (Alternatif)
        if (uiState is UiState.DownloadInitiated) {
            val state = uiState as UiState.DownloadInitiated
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        this.contentDescription = state.message
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "İndirme Başlatıldı",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AccessibleButton(
                        text = "Yeni Bir Medya İndir",
                        contentDesc = "Yeni bir bağlantı girmek için ekranı sıfırla",
                        onClick = { viewModel.resetState(); viewModel.clearInput() },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
