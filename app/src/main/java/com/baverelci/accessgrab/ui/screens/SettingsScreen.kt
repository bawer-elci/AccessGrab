package com.baverelci.accessgrab.ui.screens

import android.app.DownloadManager
import android.content.Intent
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.accessibility.AccessibilityHelper
import com.baverelci.accessgrab.data.ChangeLogData
import com.baverelci.accessgrab.downloader.ApkInstallHelper
import com.baverelci.accessgrab.ui.components.AccessibleButton
import com.baverelci.accessgrab.ui.components.ChangeLogDialog
import com.baverelci.accessgrab.ui.components.UserGuideDialog
import com.baverelci.accessgrab.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = viewModel.prefs

    val isHaptic by prefs.isHapticEnabled.collectAsState()
    val isClipboard by prefs.isClipboardEnabled.collectAsState()
    val isAutoUpdate by prefs.isAutoUpdateEnabled.collectAsState()
    val isShakePlayPause by prefs.isShakeToPlayPauseEnabled.collectAsState()
    val isCompletionSound by prefs.isCompletionSoundEnabled.collectAsState()
    val isHighContrastLargeText by prefs.isHighContrastLargeText.collectAsState()
    val defaultFormat by prefs.defaultFormat.collectAsState()

    val updateInfo by viewModel.updateInfo.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateStatusMessage by viewModel.updateStatusMessage.collectAsState()
    val isSendingLogs by viewModel.isSendingLogs.collectAsState()
    val logReportStatusMessage by viewModel.logReportStatusMessage.collectAsState()
    val isSendingFeedback by viewModel.isSendingFeedback.collectAsState()
    val feedbackStatusMessage by viewModel.feedbackStatusMessage.collectAsState()

    var isInstallingUpdate by remember { mutableStateOf(false) }
    var installProgress by remember { mutableStateOf(0) }
    var showChangeLog by remember { mutableStateOf(false) }
    var showUserGuide by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackSenderName by remember { mutableStateOf("") }
    var feedbackMessageText by remember { mutableStateOf("") }

    // Sürüm Notları (ChangeLog) Diyaloğu
    if (showChangeLog) {
        ChangeLogDialog(onDismiss = { showChangeLog = false })
    }

    // Kullanım ve Kısayol Rehberi Bilgi Kartı Diyaloğu (#33)
    if (showUserGuide) {
        UserGuideDialog(onDismiss = { showUserGuide = false })
    }

    // Geliştiriciye Öneri / Mesaj Gönder Diyaloğu (#38)
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSendingFeedback) showFeedbackDialog = false
            },
            title = {
                Text(
                    text = "Geliştiriciye Öneri / Mesaj Gönder",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Uygulama hakkındaki görüşlerinizi, yeni özellik önerilerinizi veya yaşadığınız bir sorunu doğrudan geliştiriciye iletebilirsiniz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = feedbackSenderName,
                        onValueChange = { feedbackSenderName = it },
                        label = { Text("Adınız veya Rumuzunuz (İsteğe Bağlı)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                this.contentDescription = "Adınız veya rumuzunuz metin alanı"
                            }
                    )

                    OutlinedTextField(
                        value = feedbackMessageText,
                        onValueChange = { feedbackMessageText = it },
                        label = { Text("Öneriniz veya Mesajınız") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                this.contentDescription = "Öneriniz veya mesajınız metin alanı"
                            }
                    )

                    if (feedbackStatusMessage != null) {
                        Text(
                            text = feedbackStatusMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            confirmButton = {
                AccessibleButton(
                    text = if (isSendingFeedback) "Gönderiliyor…" else "Mesajı Gönder",
                    contentDesc = if (isSendingFeedback) "Mesaj gönderiliyor" else "Yazdığınız öneri veya mesajı geliştiriciye gönder",
                    icon = Icons.Default.Send,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = !isSendingFeedback,
                    onClick = {
                        viewModel.sendUserFeedback(
                            senderName = feedbackSenderName,
                            message = feedbackMessageText,
                            onSuccess = {
                                if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                                feedbackMessageText = ""
                                showFeedbackDialog = false
                            }
                        )
                    }
                )
            },
            dismissButton = {
                if (!isSendingFeedback) {
                    AccessibleButton(
                        text = "Kapat",
                        contentDesc = "Mesaj gönderme penceresini kapat",
                        icon = Icons.Default.Close,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { showFeedbackDialog = false }
                    )
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    LaunchedEffect(updateStatusMessage) {
        updateStatusMessage?.let { msg ->
            AccessibilityHelper.announce(view, msg)
        }
    }

    LaunchedEffect(logReportStatusMessage) {
        logReportStatusMessage?.let { msg ->
            AccessibilityHelper.announce(view, msg)
        }
    }

    LaunchedEffect(feedbackStatusMessage) {
        feedbackStatusMessage?.let { msg ->
            AccessibilityHelper.announce(view, msg)
        }
    }

    // Güncelleme Bulunduğunda Açılan Erişilebilir Doğrudan Kurulum Diyaloğu
    if (updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = {
                if (!isInstallingUpdate) viewModel.dismissUpdateDialog()
            },
            title = {
                Text(
                    text = "Yeni Sürüm Mevcut (v${info.latestVersionName})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Yayınlanma Tarihi: ${info.releaseDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Yenilikler:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = info.changelog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isInstallingUpdate) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { installProgress / 100f },
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Güncelleme indiriliyor: %$installProgress",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                AccessibleButton(
                    text = if (isInstallingUpdate) "İndiriliyor (%$installProgress)…" else "Doğrudan İndir ve Kur",
                    contentDesc = if (isInstallingUpdate) "Güncelleme indiriliyor yüzde $installProgress" else "Yeni sürümü indirip doğrudan uygulamanın üzerine kur düğmesi",
                    icon = Icons.Default.Download,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = !isInstallingUpdate,
                    onClick = {
                        isInstallingUpdate = true
                        installProgress = 0
                        AccessibilityHelper.announce(view, "Güncelleme indiriliyor, bittiğinde doğrudan kurulum başlatılacak.")
                        coroutineScope.launch {
                            val downloadResult = ApkInstallHelper.downloadApk(
                                context = context,
                                apkUrl = info.apkUrl,
                                onProgress = { progress -> installProgress = progress }
                            )
                            isInstallingUpdate = false
                            if (downloadResult.isSuccess) {
                                val apkFile = downloadResult.getOrThrow()
                                if (!ApkInstallHelper.canInstallPackages(context)) {
                                    viewModel.isWaitingForInstallPermission = true
                                    AccessibilityHelper.announce(
                                        view,
                                        "Güncelleme başarıyla indirildi. Kurulumu tamamlamak için lütfen açılan ekranda 'Bu kaynaktan izin ver' seçeneğini açıp geri gelin."
                                    )
                                    ApkInstallHelper.requestInstallPermission(context)
                                } else {
                                    AccessibilityHelper.announce(view, "İndirme tamamlandı, sistem paket yükleyici açılıyor.")
                                    val installResult = ApkInstallHelper.installApk(context, apkFile)
                                    if (installResult.isSuccess) {
                                        delay(700)
                                        viewModel.dismissUpdateDialog()
                                    } else {
                                        AccessibilityHelper.announce(
                                            view,
                                            "Kurulum ekranı açılamadı: ${installResult.exceptionOrNull()?.message}"
                                        )
                                        if (isHaptic) AccessibilityHelper.vibrateError(context)
                                    }
                                }
                            } else {
                                AccessibilityHelper.announce(
                                    view,
                                    "Güncelleme indirilemedi: ${downloadResult.exceptionOrNull()?.message}"
                                )
                                if (isHaptic) AccessibilityHelper.vibrateError(context)
                            }
                        }
                    }
                )
            },
            dismissButton = {
                if (!isInstallingUpdate) {
                    AccessibleButton(
                        text = "Daha Sonra",
                        contentDesc = "Güncellemeyi ertele",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { viewModel.dismissUpdateDialog() }
                    )
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Güncelleme Olmadığında Açılan Erişilebilir Bilgilendirme Penceresi
    val isUpToDateDialogVisible by viewModel.isUpToDateDialogVisible.collectAsState()
    val latestChangelogText by viewModel.latestChangelogText.collectAsState()
    if (isUpToDateDialogVisible) {
        val currentHighlights = latestChangelogText?.takeIf { it.isNotBlank() }
            ?: ChangeLogData.releases.firstOrNull()?.highlights?.joinToString("\n") { "• $it" }
            ?: ""

        AlertDialog(
            onDismissRequest = { viewModel.dismissUpToDateDialog() },
            title = {
                Text(
                    text = "Uygulamanız Güncel (v${viewModel.currentVersionName})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AccessGrab'in en güncel sürümünü (v${viewModel.currentVersionName}) kullanıyorsunuz.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentHighlights.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bu Sürümdeki Güncelleme Özellikleri:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = currentHighlights,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                AccessibleButton(
                    text = "Tamam",
                    contentDesc = "Bilgilendirme penceresini kapat",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { viewModel.dismissUpToDateDialog() }
                )
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Güncelleme Sunucusuna Bağlanılamadığında Açılan Hata Penceresi
    val updateErrorMessage by viewModel.updateErrorMessage.collectAsState()
    if (updateErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateErrorDialog() },
            title = {
                Text(
                    text = "Bağlantı Hatası",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = updateErrorMessage!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                AccessibleButton(
                    text = "Tamam",
                    contentDesc = "Hata penceresini kapat",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { viewModel.dismissUpdateErrorDialog() }
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
        // Başlık
        Column {
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Uygulama Tercihleri ve Güncellemeler",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        // 1. Uygulama Güncellemesi ve Rehber Kartı
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Uygulama Güncellemesi",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mevcut Sürüm: ${viewModel.currentVersionName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (updateStatusMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = updateStatusMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Otomatik Güncelleme Bildirimi Anahtarı (Hoş melodili bildirim desteğiyle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = "Yeni Sürüm Bildirimi anahtarı, ${if (isAutoUpdate) "açık" else "kapalı"}."
                        }
                        .clickable {
                            val nextVal = !isAutoUpdate
                            prefs.setAutoUpdateEnabled(nextVal)
                            if (nextVal) viewModel.checkForUpdates(isManual = false)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yeni Sürüm Bildirimi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Yeni bir güncelleme çıktığında hoş bir melodiyle bildirim gönder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isAutoUpdate,
                        onCheckedChange = {
                            prefs.setAutoUpdateEnabled(it)
                            if (it) viewModel.checkForUpdates(isManual = false)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AccessibleButton(
                    text = if (isCheckingUpdate) "Güncellemeler Denetleniyor…" else "Güncellemeleri Denetle",
                    contentDesc = "Yeni sürüm olup olmadığını denetle düğmesi",
                    icon = Icons.Default.Refresh,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = !isCheckingUpdate,
                    onClick = { viewModel.checkForUpdates(isManual = true) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AccessibleButton(
                    text = "Sürüm Notları (ChangeLog)",
                    contentDesc = "Tüm sürümlerdeki yenilikleri ve değişiklik günlüğünü görüntüle",
                    icon = Icons.Default.Info,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { showChangeLog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AccessibleButton(
                    text = "Kullanım ve Kısayol Rehberi",
                    contentDesc = "AccessGrab kullanım ve TalkBack kısayol rehberi bilgi kartını aç",
                    icon = Icons.Default.HelpOutline,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { showUserGuide = true }
                )
            }
        }

        // 2. Erişilebilirlik, Yüksek Kontrast ve Titreşim Tercihleri
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Erişilebilirlik ve Geri Bildirim",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // #28 — Az Gören Kullanıcılar İçin Ultra Yüksek Kontrast (Siyah-Sarı) ve Büyük Yazı Modu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = "Ultra Yüksek Kontrast Siyah Sarı ve Dev Yazı Modu anahtarı, şu an ${if (isHighContrastLargeText) "açık" else "kapalı"}. Az gören kullanıcılar için tam siyah zemin üzerine parlak sarı dev puntolu görünüm sağlar."
                        }
                        .clickable {
                            val newVal = !isHighContrastLargeText
                            prefs.setHighContrastLargeText(newVal)
                            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ultra Yüksek Kontrast & Dev Yazı Modu",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Az görenler için tam siyah zemin üzerine parlak sarı renkler ve büyütülmüş yazılar kullan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isHighContrastLargeText,
                        onCheckedChange = {
                            prefs.setHighContrastLargeText(it)
                            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = "İşlem Titreşimi anahtarı, şu an ${if (isHaptic) "açık" else "kapalı"}. Çift dokunarak değiştirebilirsiniz."
                        }
                        .clickable {
                            val newValue = !isHaptic
                            prefs.setHapticEnabled(newValue)
                            if (newValue) AccessibilityHelper.vibrateSuccess(context)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "İşlem Titreşimi (Haptik)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "İndirme başladığında ve tamamlandığında hafif titreşim üret",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isHaptic,
                        onCheckedChange = {
                            prefs.setHapticEnabled(it)
                            if (it) AccessibilityHelper.vibrateSuccess(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // İndirme Tamamlandığında Özel Onay Sesi Anahtarı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = "İndirme Tamamlanma Sesi anahtarı, şu an ${if (isCompletionSound) "açık" else "kapalı"}. Çift dokunarak değiştirebilirsiniz."
                        }
                        .clickable {
                            val newVal = !isCompletionSound
                            prefs.setCompletionSoundEnabled(newVal)
                            if (newVal) AccessibilityHelper.playCompletionSound()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "İndirme Tamamlanma Sesi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "İndirme bittiğinde uzaktan da duyulabilen kısa onay sesi çal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isCompletionSound,
                        onCheckedChange = {
                            prefs.setCompletionSoundEnabled(it)
                            if (it) AccessibilityHelper.playCompletionSound()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Telefonu Sallayarak Duraklat / Oynat Anahtarı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = "Telefonu Sallayarak Duraklat veya Oynat anahtarı, şu an ${if (isShakePlayPause) "açık" else "kapalı"}. Çift dokunarak değiştirebilirsiniz."
                        }
                        .clickable {
                            prefs.setShakeToPlayPauseEnabled(!isShakePlayPause)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sallayarak Duraklat / Oynat",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Oynatıcı açıkken telefonu hafifçe sallayarak medyayı duraklat veya başlat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isShakePlayPause,
                        onCheckedChange = { prefs.setShakeToPlayPauseEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // 3. Akıllı Pano Algılama
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = "Panoyu Otomatik Algıla anahtarı, şu an ${if (isClipboard) "açık" else "kapalı"}."
                        }
                        .clickable { prefs.setClipboardEnabled(!isClipboard) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Panoyu Otomatik Algıla",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Uygulama açıldığında panodaki video linklerini otomatik olarak öner",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isClipboard,
                        onCheckedChange = { prefs.setClipboardEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // 4. Varsayılan İndirme Formatı (Hızlı İndirme Modu)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Varsayılan İndirme Davranışı",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val options = listOf(
                    Triple("ask", "Her Seferinde Sor (Önerilen)", "Medyayı inceledikten sonra MP4 veya MP3 seçeneğini sunar"),
                    Triple("mp4", "Doğrudan MP4 Video İndir", "Medyayı otomatik olarak en yüksek kaliteli video olarak indirir"),
                    Triple("mp3", "Doğrudan MP3 Ses İndir", "Medyayı otomatik olarak ses/podcast MP3 olarak indirir")
                )

                Column(modifier = Modifier.selectableGroup()) {
                    options.forEach { (key, label, desc) ->
                        val isSelected = defaultFormat == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .selectable(
                                    selected = isSelected,
                                    onClick = { prefs.setDefaultFormat(key) },
                                    role = Role.RadioButton
                                )
                                .semantics {
                                    this.contentDescription = "$label radyo düğmesi, ${if (isSelected) "seçili" else "seçili değil"}. $desc"
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. İletişim, Paylaşım ve İndirilenler (#37 & #38)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Paylaşım ve Geliştirici İletişimi",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                if (feedbackStatusMessage != null && !showFeedbackDialog) {
                    Text(
                        text = feedbackStatusMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                AccessibleButton(
                    text = "Geliştiriciye Öneri / Mesaj Gönder",
                    contentDesc = "Uygulama içinden geliştiriciye öneri, istek veya mesaj gönderme penceresini aç",
                    icon = Icons.Default.Feedback,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        viewModel.clearFeedbackStatus()
                        showFeedbackDialog = true
                    }
                )

                AccessibleButton(
                    text = "AccessGrab Uygulamasını Arkadaşına Paylaş",
                    contentDesc = "AccessGrab uygulamasının indirme bağlantısını WhatsApp veya diğer uygulamalarla arkadaşına paylaş",
                    icon = Icons.Default.Share,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    onClick = {
                        if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                        viewModel.shareApp(context)
                    }
                )

                AccessibleButton(
                    text = "İndirilenler Klasörünü Aç",
                    contentDesc = "Telefonun sistem indirme yöneticisini aç",
                    icon = Icons.Default.FolderOpen,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        try {
                            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }
        }

        // 6. Hata ve Tanılama Kayıtlarını Gönder (Log Gönder)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hata ve Tanılama Kayıtları",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "İndirme veya erişilebilirlik sırasında bir sorun yaşarsanız, cihaz ve uygulama hata günlüklerini (Logları) tek dokunuşla geliştirici sunucusuna gönderebilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (logReportStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = logReportStatusMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                AccessibleButton(
                    text = if (isSendingLogs) "Kayıtlar Gönderiliyor…" else "Hata Kayıtlarını (Logları) Gönder",
                    contentDesc = if (isSendingLogs) "Tanılama kayıtları gönderiliyor" else "Hata ve tanılama kayıtlarını geliştirici sunucusuna gönder",
                    icon = Icons.Default.BugReport,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = !isSendingLogs,
                    onClick = {
                        if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                        viewModel.sendDiagnosticLogs()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
