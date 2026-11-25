package com.baverelci.accessgrab.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.R
import com.baverelci.accessgrab.accessibility.AccessibilityHelper
import com.baverelci.accessgrab.player.InAppPlayerManager
import com.baverelci.accessgrab.ui.components.AccessibleButton
import com.baverelci.accessgrab.ui.components.ClipboardPromptCard
import com.baverelci.accessgrab.ui.components.DownloadCompletedCard
import com.baverelci.accessgrab.ui.components.DownloadErrorCard
import com.baverelci.accessgrab.ui.components.DownloadProgressCard
import com.baverelci.accessgrab.ui.components.InAppPlayerCard
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
    val activeAnnouncement by viewModel.activeAnnouncement.collectAsState()
    val activeTranscript by viewModel.activeTranscript.collectAsState()

    // Sunucudan Gelen Canlı Duyuru / Mesaj Penceresi (#43)
    if (activeAnnouncement != null) {
        val ann = activeAnnouncement!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissAnnouncement() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = ann.title.ifBlank { "AccessGrab Duyurusu" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ann.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                AccessibleButton(
                    text = "Kapat",
                    contentDesc = "Duyuru penceresini kapat",
                    icon = Icons.Default.Close,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { viewModel.dismissAnnouncement() }
                )
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // #60 — Videonun Konuşma Metni / Altyazı Bilgi Kartı
    if (activeTranscript != null) {
        val tr = activeTranscript!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissTranscript() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Konuşma Metni & Altyazı",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${tr.title} (${tr.language})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = tr.transcript,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AccessibleButton(
                        text = "Metni Panoya Kopyala",
                        contentDesc = "Konuşma metnini panoya kopyala",
                        icon = Icons.Default.ContentCopy,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            try {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cb?.setPrimaryClip(ClipData.newPlainText(tr.title, tr.transcript))
                                if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                                AccessibilityHelper.announce(view, "Konuşma metni panoya kopyalandı.")
                            } catch (_: Exception) {}
                        }
                    )
                }
            },
            confirmButton = {
                AccessibleButton(
                    text = "Kapat",
                    contentDesc = "Konuşma metni bilgi kartını kapat",
                    icon = Icons.Default.Close,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { viewModel.dismissTranscript() }
                )
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

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
                AccessibilityHelper.announce(view, "\"${state.query}\" için ${state.results.size} sonuç bulundu. Bir videoya dokunarak doğrudan oynatıcı penceresinde izleyebilirsiniz.")
            }
            is UiState.MediaReady -> {
                if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                val msg = "${state.data.platform} medyası hazır: ${state.data.title}. Pencerede izleyebilir veya MP4/MP3 olarak indirebilirsiniz."
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
                viewModel = viewModel,
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
    val view = LocalView.current
    val isHaptic by viewModel.prefs.isHapticEnabled.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val clipboardUrl by viewModel.clipboardDetectedUrl.collectAsState()
    val downloadQueue by viewModel.downloadQueue.collectAsState()
    val playerState by InAppPlayerManager.state.collectAsState()
    val historyItems by viewModel.prefs.historyItems.collectAsState()
    val isFetchingTranscript by viewModel.isFetchingTranscript.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }

    fun openVideoPopupPlayer(title: String, mediaUrl: String) {
        if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
        val resumedMs = InAppPlayerManager.playRemotePreview(
            title = title,
            mediaUrl = mediaUrl,
            context = context,
            prefs = viewModel.prefs
        )
        if (resumedMs > 4000) {
            val mins = (resumedMs / 1000) / 60
            val secs = (resumedMs / 1000) % 60
            AccessibilityHelper.announce(
                view,
                "$title kaldığınız yerden devam ediyor (${String.format("%02d:%02d", mins, secs)})."
            )
        } else {
            AccessibilityHelper.announce(view, "$title video oynatıcı penceresinde açılıyor…")
        }
    }

    // #71 — İzleme ve İndirme Geçmişim Diyaloğu
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Text(
                    text = "İzleme ve İndirme Geçmişim (${historyItems.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (historyItems.isEmpty()) {
                        Text(
                            text = "Henüz izlediğiniz veya indirdiğiniz bir medya geçmişi bulunmuyor.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        AccessibleButton(
                            text = "Geçmişi Temizle",
                            contentDesc = "Tüm izleme ve indirme geçmişini temizle",
                            icon = Icons.Default.DeleteForever,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                viewModel.prefs.clearWatchOrDownloadHistory()
                                if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                                AccessibilityHelper.announce(view, "İzleme ve indirme geçmişi temizlendi.")
                            }
                        )

                        historyItems.forEach { entry ->
                            val savedPos = viewModel.prefs.getSavedPlaybackPosition(entry.title)
                            val resumeBadge = if (savedPos > 4000) {
                                val mins = (savedPos / 1000) / 60
                                val secs = (savedPos / 1000) % 60
                                " • Kaldığınız yer: ${String.format("%02d:%02d", mins, secs)}"
                            } else ""
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showHistoryDialog = false
                                        openVideoPopupPlayer(entry.title, entry.url)
                                    }
                                    .semantics {
                                        this.role = Role.Button
                                        this.contentDescription = "${entry.title}. ${entry.actionType}, ${entry.timestamp}$resumeBadge. Pencerede oynatmak için çift dokunun."
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .clearAndSetSemantics { }
                                ) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${entry.actionType} • ${entry.timestamp}$resumeBadge",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AccessibleButton(
                    text = "Kapat",
                    contentDesc = "Geçmiş penceresini kapat",
                    icon = Icons.Default.Close,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { showHistoryDialog = false }
                )
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "Sosyal Medya Video & MP3 İndiricisi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Arama / Canlı Video Oynatıcı Penceresi (Görüntülü + İleri/Geri Sar + Duraklat/Oynat + İndir + Kaldığı Yerden Devam Et)
        if (playerState.isActive && playerState.isRemotePreview) {
            InAppPlayerCard(
                playerState = playerState,
                onTogglePlayPause = { InAppPlayerManager.togglePlayPause() },
                onSeekBackward = {
                    InAppPlayerManager.seekBy(-10_000)
                    AccessibilityHelper.announce(view, "10 saniye geri sarıldı")
                },
                onSeekForward = {
                    InAppPlayerManager.seekBy(10_000)
                    AccessibilityHelper.announce(view, "10 saniye ileri sarıldı")
                },
                onRestartFromBeginning = {
                    InAppPlayerManager.restartFromBeginning()
                    AccessibilityHelper.announce(view, "En baştan başlatıldı")
                },
                onNext = {},
                onPrevious = {},
                onCycleSpeed = {
                    val newSpeed = InAppPlayerManager.cyclePlaybackSpeed()
                    val formatted = if (newSpeed % 1.0f == 0f) "${newSpeed.toInt()}x" else "${newSpeed}x"
                    AccessibilityHelper.announce(view, "Oynatma hızı: $formatted")
                },
                onCycleVolumeBoost = {
                    val newBoost = InAppPlayerManager.cycleVolumeBoost()
                    AccessibilityHelper.announce(view, "Ses gücü: %$newBoost")
                },
                onDownloadRemoteMp4 = { title, url ->
                    InAppPlayerManager.stop()
                    viewModel.selectFormat("mp4")
                    viewModel.selectQuality("720")
                    viewModel.startTrackedDownload(
                        context = context,
                        mediaUrl = url,
                        format = "mp4",
                        title = title,
                        quality = "720"
                    )
                },
                onDownloadRemoteMp3 = { title, url ->
                    InAppPlayerManager.stop()
                    viewModel.selectFormat("mp3")
                    viewModel.selectQuality("192")
                    viewModel.startTrackedDownload(
                        context = context,
                        mediaUrl = url,
                        format = "mp3",
                        title = title,
                        quality = "192"
                    )
                },
                onStop = {
                    InAppPlayerManager.stop()
                    AccessibilityHelper.announce(view, "Video oynatıcı penceresi kapatıldı")
                }
            )
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

        // #73 — Popüler Müzikler / Günün Trendleri & #71 — İzleme ve İndirme Geçmişim Hızlı Erişim Satırı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccessibleButton(
                text = "Günün Trendleri",
                contentDesc = "Günün trendlerini ve en popüler müzikleri tek dokunuşla listele",
                icon = Icons.Default.TrendingUp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                    viewModel.loadTrendingMusic()
                }
            )

            AccessibleButton(
                text = "Geçmişim (${historyItems.size})",
                contentDesc = "İzleme ve indirme geçmişim listesini aç (${historyItems.size} kayıt)",
                icon = Icons.Default.History,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
                onClick = {
                    showHistoryDialog = true
                }
            )
        }

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

        // Arama Sonuçları ve Oynatma Listesi (Playlist) Kartı
        if (uiState is UiState.SearchResultsReady) {
            val searchState = uiState as UiState.SearchResultsReady
            SearchResultsCard(
                query = searchState.query,
                results = searchState.results,
                canLoadMore = searchState.canLoadMore,
                isLoadingMore = searchState.isLoadingMore,
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
                },
                onPreviewListen = { item ->
                    openVideoPopupPlayer(item.title, item.url)
                },
                onDownloadAllList = { fmt, qual ->
                    viewModel.downloadAllCurrentResults(context, fmt, qual)
                },
                onReadTranscript = { item ->
                    viewModel.fetchVideoTranscript(item.url)
                },
                getSavedPositionMs = { title ->
                    viewModel.prefs.getSavedPlaybackPosition(title)
                },
                onLoadMore = {
                    viewModel.loadMoreSearchResults()
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
                },
                onPreviewListen = {
                    openVideoPopupPlayer(media.title, media.url)
                },
                onReadTranscript = {
                    viewModel.fetchVideoTranscript(media.url)
                },
                isFetchingTranscript = isFetchingTranscript
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
