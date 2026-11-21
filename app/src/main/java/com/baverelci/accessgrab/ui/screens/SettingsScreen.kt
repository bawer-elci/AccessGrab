package com.baverelci.accessgrab.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.baverelci.accessgrab.downloader.ApkInstallHelper
import com.baverelci.accessgrab.ui.components.AccessibleButton
import com.baverelci.accessgrab.ui.components.ChangeLogDialog
import com.baverelci.accessgrab.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val defaultFormat by prefs.defaultFormat.collectAsState()

    val updateInfo by viewModel.updateInfo.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateStatusMessage by viewModel.updateStatusMessage.collectAsState()

    var isInstallingUpdate by remember { mutableStateOf(false) }
    var installProgress by remember { mutableStateOf(0) }
    var showChangeLog by remember { mutableStateOf(false) }

    // Sürüm Notları (ChangeLog) Diyaloğu
    if (showChangeLog) {
        ChangeLogDialog(onDismiss = { showChangeLog = false })
    }

    LaunchedEffect(updateStatusMessage) {
        updateStatusMessage?.let { msg ->
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
    if (isUpToDateDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpToDateDialog() },
            title = {
                Text(
                    text = "Uygulamanız Güncel",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "Şu anda yeni bir sürüm mevcut değil. AccessGrab'in en güncel sürümünü (v${viewModel.currentVersionName}) kullanıyorsunuz.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

        // 1. Uygulama Güncellemesi Kartı
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

                // Otomatik Güncelleme Bildirimi Anahtarı (Kullanıcı isteğiyle varsayılan KAPALI)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = "Yeni Sürüm Bildirimi anahtarı, ${if (isAutoUpdate) "açık" else "kapalı"}."
                        }
                        .clickable { prefs.setAutoUpdateEnabled(!isAutoUpdate) },
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
                            text = "Yeni bir güncelleme çıktığında telefonuma bildirim gönder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isAutoUpdate,
                        onCheckedChange = { prefs.setAutoUpdateEnabled(it) },
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
            }
        }

        // 2. Erişilebilirlik ve Titreşim Tercihleri
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
                        text = "Dokunsal Geri Bildirim",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

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

        // 5. İndirilen Dosyalara Hızlı Erişim
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "İndirilen Dosyalar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "İndirdiğiniz videolar ve sesler telefonunuzun İndirilenler klasöründeki AccessGrab alt dizininde saklanır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}
