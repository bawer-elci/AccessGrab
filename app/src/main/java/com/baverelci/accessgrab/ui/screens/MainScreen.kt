package com.baverelci.accessgrab.ui.screens

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.baverelci.accessgrab.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.accessibility.AccessibilityHelper
import com.baverelci.accessgrab.ui.components.AccessibleButton
import com.baverelci.accessgrab.ui.components.MediaInfoCard
import com.baverelci.accessgrab.viewmodel.MainViewModel
import com.baverelci.accessgrab.viewmodel.UiState

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentTab by viewModel.currentTab.collectAsState()
    val isHaptic by viewModel.prefs.isHapticEnabled.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()

    // TalkBack canlı durum bildirimleri
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Loading -> {
                AccessibilityHelper.announce(view, state.message)
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

                // 2. Ayarlar Sekmesi
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ayarlar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.semantics {
                        this.contentDescription = "Ayarlar sekmesi, ${if (currentTab == 1) "seçili" else "seçili değil"}"
                    }
                )

                // 3. Hakkında Sekmesi
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Hakkında") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.semantics {
                        this.contentDescription = "Hakkında sekmesi, ${if (currentTab == 2) "seçili" else "seçili değil"}"
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
            1 -> SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            2 -> AboutScreen(
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
    val urlInput by viewModel.urlInput.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val clipboardUrl by viewModel.clipboardDetectedUrl.collectAsState()

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
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Panoda medya bağlantısı algılandı:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = clipboardUrl ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AccessibleButton(
                            text = "Bu Bağlantıyı Kullan",
                            contentDesc = "Panodaki bağlantıyı kullan ve medyayı getir",
                            onClick = { viewModel.applyDetectedClipboardUrl() },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        AccessibleButton(
                            text = "Kapat",
                            contentDesc = "Pano önerisini kapat",
                            onClick = { viewModel.dismissClipboardPrompt() },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                }
            }
        }

        // URL Giriş Alanı
        OutlinedTextField(
            value = urlInput,
            onValueChange = { viewModel.onUrlChanged(it) },
            label = { Text("Bağlantı Adresi (URL)") },
            placeholder = { Text("Instagram, TikTok, YouTube, X, Facebook linki") },
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            trailingIcon = {
                Row {
                    if (urlInput.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearInput() },
                            modifier = Modifier.semantics {
                                this.contentDescription = "Metin alanını temizle düğmesi"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clip.isNotBlank()) {
                                viewModel.onUrlChanged(clip)
                            }
                        },
                        modifier = Modifier.semantics {
                            this.contentDescription = "Panodan yapıştır düğmesi"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = "Sosyal medya bağlantısı giriş kutusu. ${if (urlInput.isEmpty()) "Şu an boş" else "İçerik: $urlInput"}"
                }
        )

        // Medyayı İncele Butonu
        AccessibleButton(
            text = "Medyayı İncele",
            contentDesc = "Girilen bağlantıdaki medyayı incele ve indirme seçeneklerini getir",
            icon = Icons.Default.Search,
            onClick = { viewModel.fetchMediaInfo() },
            enabled = uiState !is UiState.Loading
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

        // Hata Alanı
        if (uiState is UiState.Error) {
            val errorMsg = (uiState as UiState.Error).message
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        this.liveRegion = LiveRegionMode.Assertive
                        this.contentDescription = "Hata mesajı: $errorMsg"
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Medya Başarıyla Yüklendiğinde İndirme Seçenekleri
        if (uiState is UiState.MediaReady) {
            val media = (uiState as UiState.MediaReady).data

            MediaInfoCard(media = media)

            Text(
                text = "Format ve Kalite Seçimi",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )

            // Format Seçim Düğmeleri (Video vs Ses)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isVideoSelected = selectedFormat == "mp4"
                AccessibleButton(
                    text = if (isVideoSelected) "✓ Video (MP4)" else "Video (MP4)",
                    contentDesc = "Video formatı ${if (isVideoSelected) "seçili" else "seçmek için dokunun"}",
                    icon = Icons.Default.VideoLibrary,
                    containerColor = if (isVideoSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isVideoSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectFormat("mp4") }
                )

                val isAudioSelected = selectedFormat == "mp3"
                AccessibleButton(
                    text = if (isAudioSelected) "✓ Ses (MP3)" else "Ses (MP3)",
                    contentDesc = "Ses formatı ${if (isAudioSelected) "seçili" else "seçmek için dokunun"}",
                    icon = Icons.Default.Audiotrack,
                    containerColor = if (isAudioSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isAudioSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectFormat("mp3") }
                )
            }

            // Kalite Seçenekleri Kartı
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val qualityTitle = if (selectedFormat == "mp4") "Çözünürlük Seçeneği" else "Ses Kalitesi (Bit Hızı)"
                    Text(
                        text = qualityTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedFormat == "mp4") {
                        val videoQualities = listOf(
                            Triple("1080", "1080p FHD", "En yüksek netlik ve çözünürlük"),
                            Triple("720", "720p HD", "Standart yüksek kalite (Önerilen)"),
                            Triple("480", "480p SD", "Standart kalite, hızlı indirme"),
                            Triple("360", "360p", "Düşük veri kullanımı ve küçük dosya boyutu")
                        )

                        videoQualities.forEach { (qKey, qLabel, qDesc) ->
                            val isSelected = selectedQuality == qKey
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectQuality(qKey) }
                                    .semantics {
                                        this.role = Role.RadioButton
                                        this.selected = isSelected
                                        this.contentDescription = "$qLabel, $qDesc. ${if (isSelected) "Seçili" else "Seçmek için çift dokunun"}"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectQuality(qKey) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = qLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = qDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val audioQualities = listOf(
                            Triple("320", "320 kbps (Yüksek Kalite)", "Stüdyo kalitesinde kristal netliğinde ses"),
                            Triple("192", "192 kbps (Standart)", "Müzik ve videolar için en dengeli ses (Önerilen)"),
                            Triple("128", "128 kbps (Tasarruf)", "Podcast ve konuşmalar için ideal, düşük boyut")
                        )

                        audioQualities.forEach { (qKey, qLabel, qDesc) ->
                            val isSelected = selectedQuality == qKey
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectQuality(qKey) }
                                    .semantics {
                                        this.role = Role.RadioButton
                                        this.selected = isSelected
                                        this.contentDescription = "$qLabel, $qDesc. ${if (isSelected) "Seçili" else "Seçmek için çift dokunun"}"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectQuality(qKey) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = qLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = qDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ana İndirme Butonu
            val isVid = selectedFormat == "mp4"
            val downloadBtnText = if (isVid) "$selectedQuality" + "p Videoyu İndir (MP4)" else "$selectedQuality kbps Sesi İndir (MP3)"
            AccessibleButton(
                text = downloadBtnText,
                contentDesc = "$downloadBtnText, indirme işlemini başlat",
                icon = Icons.Default.Download,
                containerColor = if (isVid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                contentColor = if (isVid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                onClick = {
                    viewModel.startTrackedDownload(
                        context = context,
                        mediaUrl = media.url,
                        format = selectedFormat,
                        title = media.title,
                        quality = selectedQuality
                    )
                }
            )
        }

        // 1. Canlı İlerleme Çubuğu ve İndirme Yüzdesi Göstergesi
        if (uiState is UiState.Downloading) {
            val state = uiState as UiState.Downloading
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        this.liveRegion = LiveRegionMode.Polite
                        this.contentDescription = "${state.title} indiriliyor. İlerleme: Yüzde ${state.progress}"
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "İndiriliyor (${state.format})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "%${state.progress}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )

                    // Canlı İlerleme Çubuğu
                    if (state.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatBytes(state.downloadedBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatBytes(state.totalBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // 2. İndirme Tamamlandı Kartı
        if (uiState is UiState.DownloadCompleted) {
            val state = uiState as UiState.DownloadCompleted
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        this.liveRegion = LiveRegionMode.Polite
                        this.contentDescription = "İndirme tamamlandı! ${state.title}"
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "İndirme Tamamlandı! (%100)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = state.format,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AccessibleButton(
                            text = "Dosyayı Aç",
                            contentDesc = "İndirilenler klasörünü aç ve dosyayı gör",
                            icon = Icons.Default.FolderOpen,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                try {
                                    val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )

                        AccessibleButton(
                            text = "Yeni Medya",
                            contentDesc = "Yeni bir medya indirmek için sıfırla",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.resetState()
                                viewModel.clearInput()
                            }
                        )
                    }
                }
            }
        }

        // 3. İndirme Başlatıldı Bildirimi (Alternatif)
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
