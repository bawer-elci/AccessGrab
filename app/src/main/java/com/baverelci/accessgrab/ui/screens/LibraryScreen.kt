package com.baverelci.accessgrab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.accessibility.AccessibilityHelper
import com.baverelci.accessgrab.data.DownloadedMediaFile
import com.baverelci.accessgrab.data.MediaFileManager
import com.baverelci.accessgrab.data.RingtoneHelper
import com.baverelci.accessgrab.player.InAppPlayerManager
import com.baverelci.accessgrab.ui.components.AccessibleButton
import com.baverelci.accessgrab.ui.components.InAppPlayerCard
import com.baverelci.accessgrab.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHaptic by viewModel.prefs.isHapticEnabled.collectAsState()
    val activeRingtonePath by viewModel.prefs.activeRingtonePath.collectAsState()
    val activeAlarmPath by viewModel.prefs.activeAlarmPath.collectAsState()
    val activeNotificationSoundPath by viewModel.prefs.activeNotificationSoundPath.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val playerState by InAppPlayerManager.state.collectAsState()

    var files by remember { mutableStateOf(MediaFileManager.getDownloadedFiles(context)) }
    var fileToDelete by remember { mutableStateOf<DownloadedMediaFile?>(null) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var fileForOptionsMenu by remember { mutableStateOf<DownloadedMediaFile?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    fun syncFiles() {
        files = MediaFileManager.getDownloadedFiles(context)
    }

    LaunchedEffect(uiState) {
        syncFiles()
    }

    LaunchedEffect(Unit) {
        while (true) {
            syncFiles()
            delay(1500)
        }
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            AccessibilityHelper.announce(view, msg)
            feedbackMessage = null
        }
    }

    fun handlePlay(item: DownloadedMediaFile, fromBeginning: Boolean = false) {
        if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
        val resumedMs = InAppPlayerManager.playMedia(
            target = item,
            currentList = files,
            context = context,
            prefs = viewModel.prefs,
            forceFromStart = fromBeginning
        )
        if (resumedMs > 5000 && !fromBeginning) {
            val mins = (resumedMs / 1000) / 60
            val secs = (resumedMs / 1000) % 60
            feedbackMessage = "${item.name} kaldığınız yerden devam ediyor (${String.format("%02d:%02d", mins, secs)})."
        } else {
            feedbackMessage = "${item.name} oynatılıyor."
        }
    }

    fun handleSetRingtone(item: DownloadedMediaFile) {
        val res = RingtoneHelper.setAsRingtone(context, item.file, viewModel.prefs)
        if (res.isSuccess) {
            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
            feedbackMessage = res.getOrNull()
        } else {
            if (isHaptic) AccessibilityHelper.vibrateError(context)
            feedbackMessage = res.exceptionOrNull()?.message
        }
    }

    fun handleRemoveRingtone() {
        val res = RingtoneHelper.removeCustomRingtone(context, viewModel.prefs)
        if (res.isSuccess) {
            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
            feedbackMessage = res.getOrNull()
        } else {
            if (isHaptic) AccessibilityHelper.vibrateError(context)
            feedbackMessage = res.exceptionOrNull()?.message
        }
    }

    fun handleSetAlarm(item: DownloadedMediaFile) {
        val res = RingtoneHelper.setAsAlarm(context, item.file, viewModel.prefs)
        if (res.isSuccess) {
            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
            feedbackMessage = res.getOrNull()
        } else {
            if (isHaptic) AccessibilityHelper.vibrateError(context)
            feedbackMessage = res.exceptionOrNull()?.message
        }
    }

    fun handleRemoveAlarm() {
        val res = RingtoneHelper.removeCustomAlarm(context, viewModel.prefs)
        if (res.isSuccess) {
            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
            feedbackMessage = res.getOrNull()
        } else {
            if (isHaptic) AccessibilityHelper.vibrateError(context)
            feedbackMessage = res.exceptionOrNull()?.message
        }
    }

    fun handleSetNotificationSound(item: DownloadedMediaFile) {
        val res = RingtoneHelper.setAsNotificationSound(context, item.file, viewModel.prefs)
        if (res.isSuccess) {
            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
            feedbackMessage = res.getOrNull()
        } else {
            if (isHaptic) AccessibilityHelper.vibrateError(context)
            feedbackMessage = res.exceptionOrNull()?.message
        }
    }

    fun handleRemoveNotificationSound() {
        val res = RingtoneHelper.removeCustomNotificationSound(context, viewModel.prefs)
        if (res.isSuccess) {
            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
            feedbackMessage = res.getOrNull()
        } else {
            if (isHaptic) AccessibilityHelper.vibrateError(context)
            feedbackMessage = res.exceptionOrNull()?.message
        }
    }

    // "Tümünü Sil" Onay Diyaloğu
    if (showDeleteAllConfirm) {
        val totalCount = files.size
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = {
                Text(
                    text = "Tüm Medyaları Sil",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Kütüphanedeki toplam $totalCount adet medya dosyasının tamamını cihazınızdan kalıcı olarak silmek istediğinize emin misiniz?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                AccessibleButton(
                    text = "Evet, Tümünü Sil",
                    contentDesc = "Kütüphanedeki toplam $totalCount adet medya dosyasını kalıcı olarak sil",
                    icon = Icons.Default.DeleteForever,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = {
                        InAppPlayerManager.stop()
                        val deletedCount = MediaFileManager.deleteAllMediaFiles(context)
                        files.forEach { viewModel.prefs.clearSavedPlaybackPosition(it.name) }
                        if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                        feedbackMessage = "$deletedCount adet medya dosyası kütüphaneden silindi."
                        syncFiles()
                        showDeleteAllConfirm = false
                    }
                )
            },
            dismissButton = {
                AccessibleButton(
                    text = "İptal",
                    contentDesc = "Tümünü silme işlemini iptal et",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { showDeleteAllConfirm = false }
                )
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // "Diğer Seçenekler" Menüsü Diyaloğu (Oynat, Baştan Başlat, Paylaş, Zil Sesi / Alarm Sesi / Bildirim Sesi Yap-Kaldır, Harici Aç, Sil)
    if (fileForOptionsMenu != null) {
        val target = fileForOptionsMenu!!
        val isThisRingtone = activeRingtonePath == target.file.absolutePath
        val hasAnyCustomRingtone = !activeRingtonePath.isNullOrBlank()
        val isThisAlarm = activeAlarmPath == target.file.absolutePath
        val hasAnyCustomAlarm = !activeAlarmPath.isNullOrBlank()
        val isThisNotificationSound = activeNotificationSoundPath == target.file.absolutePath
        val hasAnyCustomNotificationSound = !activeNotificationSoundPath.isNullOrBlank()
        val savedPos = viewModel.prefs.getSavedPlaybackPosition(target.name)

        AlertDialog(
            onDismissRequest = { fileForOptionsMenu = null },
            title = {
                Text(
                    text = target.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${target.extension} • ${target.formattedSize} • ${target.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    AccessibleButton(
                        text = if (savedPos > 5000) "Kaldığım Yerden Oynat" else "Oynat",
                        contentDesc = "${target.name} dosyasını uygulama içinde oynat",
                        icon = Icons.Default.PlayArrow,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = {
                            val item = target
                            fileForOptionsMenu = null
                            handlePlay(item, fromBeginning = false)
                        }
                    )

                    if (savedPos > 5000) {
                        AccessibleButton(
                            text = "Baştan Başlat",
                            contentDesc = "${target.name} dosyasını en baştan başlat",
                            icon = Icons.Default.Replay,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                val item = target
                                fileForOptionsMenu = null
                                handlePlay(item, fromBeginning = true)
                            }
                        )
                    }

                    AccessibleButton(
                        text = "Paylaş",
                        contentDesc = "${target.name} dosyasını paylaş",
                        icon = Icons.Default.Share,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            val item = target
                            fileForOptionsMenu = null
                            val result = MediaFileManager.shareMediaFile(context, item.file)
                            if (result.isFailure) {
                                if (isHaptic) AccessibilityHelper.vibrateError(context)
                                feedbackMessage = "Paylaşım ekranı başlatılamadı."
                            }
                        }
                    )

                    // 1. Zil Sesi Yap / Kaldır
                    if (!isThisRingtone) {
                        AccessibleButton(
                            text = "Zil Sesi Yap",
                            contentDesc = "${target.name} dosyasını telefon zil sesi yap",
                            icon = Icons.Default.NotificationsActive,
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                val item = target
                                fileForOptionsMenu = null
                                handleSetRingtone(item)
                            }
                        )
                    }

                    if (isThisRingtone || hasAnyCustomRingtone) {
                        AccessibleButton(
                            text = "Zil Sesini Kaldır (Eskiye Dön)",
                            contentDesc = "Ayarlanan zil sesini kaldır ve önceki zil sesine dön",
                            icon = Icons.Default.NotificationsOff,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                fileForOptionsMenu = null
                                handleRemoveRingtone()
                            }
                        )
                    }

                    // 2. Alarm Sesi Yap / Kaldır (#21)
                    if (!isThisAlarm) {
                        AccessibleButton(
                            text = "Alarm Sesi Yap",
                            contentDesc = "${target.name} dosyasını telefon alarm sesi yap",
                            icon = Icons.Default.Alarm,
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                val item = target
                                fileForOptionsMenu = null
                                handleSetAlarm(item)
                            }
                        )
                    }

                    if (isThisAlarm || hasAnyCustomAlarm) {
                        AccessibleButton(
                            text = "Alarm Sesini Kaldır (Eskiye Dön)",
                            contentDesc = "Ayarlanan alarm sesini kaldır ve önceki alarm sesine dön",
                            icon = Icons.Default.AlarmOff,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                fileForOptionsMenu = null
                                handleRemoveAlarm()
                            }
                        )
                    }

                    // 3. Bildirim Sesi Yap / Kaldır (#21)
                    if (!isThisNotificationSound) {
                        AccessibleButton(
                            text = "Bildirim Sesi Yap",
                            contentDesc = "${target.name} dosyasını telefon bildirim sesi yap",
                            icon = Icons.Default.Notifications,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                val item = target
                                fileForOptionsMenu = null
                                handleSetNotificationSound(item)
                            }
                        )
                    }

                    if (isThisNotificationSound || hasAnyCustomNotificationSound) {
                        AccessibleButton(
                            text = "Bildirim Sesini Kaldır (Eskiye Dön)",
                            contentDesc = "Ayarlanan bildirim sesini kaldır ve önceki bildirim sesine dön",
                            icon = Icons.Default.NotificationsOff,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                fileForOptionsMenu = null
                                handleRemoveNotificationSound()
                            }
                        )
                    }

                    AccessibleButton(
                        text = "Harici Oynatıcıda Aç",
                        contentDesc = "${target.name} dosyasını telefonun harici oynatıcısında aç",
                        icon = Icons.Default.OpenInNew,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            val item = target
                            fileForOptionsMenu = null
                            val result = MediaFileManager.openMediaFile(context, item.file)
                            if (result.isFailure) {
                                if (isHaptic) AccessibilityHelper.vibrateError(context)
                                feedbackMessage = "Dosya açılamadı: Uygun bir harici medya oynatıcı bulunamadı."
                            }
                        }
                    )

                    AccessibleButton(
                        text = "Sil",
                        contentDesc = "${target.name} dosyasını cihazdan sil",
                        icon = Icons.Default.Delete,
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            val item = target
                            fileForOptionsMenu = null
                            fileToDelete = item
                        }
                    )
                }
            },
            confirmButton = {
                AccessibleButton(
                    text = "Kapat",
                    contentDesc = "Seçenekler menüsünü kapat",
                    icon = Icons.Default.Close,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { fileForOptionsMenu = null }
                )
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Tekil Silme Onay Diyaloğu
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = {
                Text(
                    text = "Dosyayı Sil",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "\"${target.name}.${target.extension.lowercase()}\" dosyasını cihazınızdan kalıcı olarak silmek istediğinize emin misiniz?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                AccessibleButton(
                    text = "Evet, Sil",
                    contentDesc = "${target.name} dosyasını kalıcı olarak sil",
                    icon = Icons.Default.Delete,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = {
                        if (playerState.currentFile?.file?.absolutePath == target.file.absolutePath) {
                            InAppPlayerManager.stop()
                        }
                        val deleted = MediaFileManager.deleteMediaFile(target.file, context)
                        if (deleted) {
                            viewModel.prefs.clearSavedPlaybackPosition(target.name)
                            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                            feedbackMessage = "${target.name} dosyası silindi."
                            syncFiles()
                        } else {
                            if (isHaptic) AccessibilityHelper.vibrateError(context)
                            feedbackMessage = "Dosya silinemedi."
                        }
                        fileToDelete = null
                    }
                )
            },
            dismissButton = {
                AccessibleButton(
                    text = "İptal",
                    contentDesc = "Silme işlemini iptal et",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { fileToDelete = null }
                )
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Kütüphanem",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (files.isEmpty()) "Henüz indirilmiş medya yok" else "Toplam ${files.size} adet indirilen medya dosyası",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }

            if (files.isNotEmpty()) {
                AccessibleButton(
                    text = "Tümünü Sil",
                    contentDesc = "Kütüphanedeki toplam ${files.size} adet medya dosyasının tamamını sil",
                    icon = Icons.Default.DeleteForever,
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.error,
                    fullWidth = false,
                    onClick = {
                        showDeleteAllConfirm = true
                    }
                )
            }
        }

        // Görüntülü Video & Ses Oynatıcı Penceresi (Aktifse ekranda açılır)
        if (playerState.isActive) {
            InAppPlayerCard(
                playerState = playerState,
                onTogglePlayPause = {
                    InAppPlayerManager.togglePlayPause()
                },
                onSeekBackward = {
                    InAppPlayerManager.seekBy(-10_000)
                    feedbackMessage = "10 saniye geri sarıldı"
                },
                onSeekForward = {
                    InAppPlayerManager.seekBy(10_000)
                    feedbackMessage = "10 saniye ileri sarıldı"
                },
                onRestartFromBeginning = {
                    InAppPlayerManager.restartFromBeginning()
                    feedbackMessage = "En baştan başlatıldı"
                },
                onNext = {
                    if (InAppPlayerManager.playNext()) {
                        feedbackMessage = "Sonraki medyaya geçildi"
                    } else {
                        feedbackMessage = "Listenin sonundasınız"
                    }
                },
                onPrevious = {
                    if (InAppPlayerManager.playPrevious()) {
                        feedbackMessage = "Önceki medyaya geçildi"
                    } else {
                        feedbackMessage = "Listenin başındasınız"
                    }
                },
                onCycleSpeed = {
                    val newSpeed = InAppPlayerManager.cyclePlaybackSpeed()
                    val formatted = if (newSpeed % 1.0f == 0f) "${newSpeed.toInt()}x" else "${newSpeed}x"
                    feedbackMessage = "Oynatma hızı: $formatted"
                },
                onCycleVolumeBoost = {
                    val newBoost = InAppPlayerManager.cycleVolumeBoost()
                    feedbackMessage = "Ses gücü: %$newBoost"
                },
                onClip30SecRingtone = {
                    val currentFile = playerState.currentFile
                    if (currentFile != null) {
                        val res = RingtoneHelper.trim30SecAndSetRingtone(
                            context = context,
                            sourceFile = currentFile.file,
                            startMs = playerState.currentPositionMs,
                            totalDurationMs = playerState.durationMs,
                            prefs = viewModel.prefs
                        )
                        if (res.isSuccess) {
                            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                            feedbackMessage = res.getOrNull()
                            syncFiles()
                        } else {
                            if (isHaptic) AccessibilityHelper.vibrateError(context)
                            feedbackMessage = res.exceptionOrNull()?.message
                        }
                    }
                },
                onStop = {
                    InAppPlayerManager.stop()
                    feedbackMessage = "Oynatıcı kapatıldı"
                }
            )
        }

        if (files.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        this.contentDescription = "Kütüphaneniz şu an boş. İndir sekmesinden indirdiğiniz tüm video ve sesler burada otomatik olarak listelenecektir."
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clearAndSetSemantics { },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Henüz İndirilmiş Medya Yok",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "İndir sekmesinden indirdiğiniz tüm MP4 videolar ve MP3 sesler telefonunuzun AccessGrab klasörüne kaydedilir ve burada otomatik olarak listelenir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(files, key = { it.file.absolutePath }) { item ->
                    val isRingtone = activeRingtonePath == item.file.absolutePath
                    val isAlarm = activeAlarmPath == item.file.absolutePath
                    val isNotificationSound = activeNotificationSoundPath == item.file.absolutePath
                    val savedPos = viewModel.prefs.getSavedPlaybackPosition(item.name)

                    MediaFileItemCard(
                        item = item,
                        isCurrentRingtone = isRingtone,
                        isCurrentAlarm = isAlarm,
                        isCurrentNotificationSound = isNotificationSound,
                        savedPositionMs = savedPos,
                        onOpen = {
                            handlePlay(item, fromBeginning = false)
                        },
                        onRestartFromBeginning = {
                            handlePlay(item, fromBeginning = true)
                        },
                        onOpenOptionsMenu = {
                            fileForOptionsMenu = item
                        },
                        onSetRingtone = {
                            handleSetRingtone(item)
                        },
                        onRemoveRingtone = {
                            handleRemoveRingtone()
                        },
                        onSetAlarm = {
                            handleSetAlarm(item)
                        },
                        onRemoveAlarm = {
                            handleRemoveAlarm()
                        },
                        onSetNotificationSound = {
                            handleSetNotificationSound(item)
                        },
                        onRemoveNotificationSound = {
                            handleRemoveNotificationSound()
                        },
                        onOpenExternal = {
                            val result = MediaFileManager.openMediaFile(context, item.file)
                            if (result.isFailure) {
                                if (isHaptic) AccessibilityHelper.vibrateError(context)
                                feedbackMessage = "Dosya açılamadı: Uygun bir harici medya oynatıcı bulunamadı."
                            }
                        },
                        onShare = {
                            val result = MediaFileManager.shareMediaFile(context, item.file)
                            if (result.isFailure) {
                                if (isHaptic) AccessibilityHelper.vibrateError(context)
                                feedbackMessage = "Paylaşım ekranı başlatılamadı."
                            }
                        },
                        onDelete = {
                            fileToDelete = item
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaFileItemCard(
    item: DownloadedMediaFile,
    isCurrentRingtone: Boolean = false,
    isCurrentAlarm: Boolean = false,
    isCurrentNotificationSound: Boolean = false,
    savedPositionMs: Int = 0,
    onOpen: () -> Unit,
    onRestartFromBeginning: () -> Unit,
    onOpenOptionsMenu: () -> Unit,
    onSetRingtone: () -> Unit,
    onRemoveRingtone: () -> Unit,
    onSetAlarm: () -> Unit,
    onRemoveAlarm: () -> Unit,
    onSetNotificationSound: () -> Unit,
    onRemoveNotificationSound: () -> Unit,
    onOpenExternal: (() -> Unit)? = null,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeLabel = if (item.isAudio) "MP3 Ses" else "MP4 Video"
    val toneBadges = buildList {
        if (isCurrentRingtone) add("Aktif Zil Sesi")
        if (isCurrentAlarm) add("Aktif Alarm Sesi")
        if (isCurrentNotificationSound) add("Aktif Bildirim Sesi")
    }
    val ringtoneBadge = if (toneBadges.isNotEmpty()) " (${toneBadges.joinToString(", ")})" else ""
    val resumeBadge = if (savedPositionMs > 5000) {
        val mins = (savedPositionMs / 1000) / 60
        val secs = (savedPositionMs / 1000) % 60
        " • Kaldığınız yer: ${String.format("%02d:%02d", mins, secs)}"
    } else ""

    val accessibleDesc = "$typeLabel$ringtoneBadge: ${item.name}. Boyut: ${item.formattedSize}. Tarih: ${item.formattedDate}$resumeBadge."

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sol/Orta Alan: Dosya bilgisi (Tek öğe olarak okunur, çift dokununca oynatır ve TalkBack Eylemler menüsü içerir)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen)
                        .semantics {
                            this.contentDescription = accessibleDesc
                            this.role = Role.Button
                            val actions = mutableListOf(
                                CustomAccessibilityAction(
                                    label = if (savedPositionMs > 5000) "Kaldığım Yerden Oynat" else "Oynat"
                                ) {
                                    onOpen()
                                    true
                                }
                            )
                            if (savedPositionMs > 5000) {
                                actions.add(
                                    CustomAccessibilityAction(label = "Baştan Başlat") {
                                        onRestartFromBeginning()
                                        true
                                    }
                                )
                            }
                            actions.add(
                                CustomAccessibilityAction(label = "Paylaş") {
                                    onShare()
                                    true
                                }
                            )
                            if (!isCurrentRingtone) {
                                actions.add(
                                    CustomAccessibilityAction(label = "Zil Sesi Yap") {
                                        onSetRingtone()
                                        true
                                    }
                                )
                            } else {
                                actions.add(
                                    CustomAccessibilityAction(label = "Zil Sesini Kaldır") {
                                        onRemoveRingtone()
                                        true
                                    }
                                )
                            }
                            if (!isCurrentAlarm) {
                                actions.add(
                                    CustomAccessibilityAction(label = "Alarm Sesi Yap") {
                                        onSetAlarm()
                                        true
                                    }
                                )
                            } else {
                                actions.add(
                                    CustomAccessibilityAction(label = "Alarm Sesini Kaldır") {
                                        onRemoveAlarm()
                                        true
                                    }
                                )
                            }
                            if (!isCurrentNotificationSound) {
                                actions.add(
                                    CustomAccessibilityAction(label = "Bildirim Sesi Yap") {
                                        onSetNotificationSound()
                                        true
                                    }
                                )
                            } else {
                                actions.add(
                                    CustomAccessibilityAction(label = "Bildirim Sesini Kaldır") {
                                        onRemoveNotificationSound()
                                        true
                                    }
                                )
                            }
                            actions.add(
                                CustomAccessibilityAction(label = "Diğer Seçenekler Menüsü") {
                                    onOpenOptionsMenu()
                                    true
                                }
                            )
                            if (onOpenExternal != null) {
                                actions.add(
                                    CustomAccessibilityAction(label = "Harici Oynatıcıda Aç") {
                                        onOpenExternal()
                                        true
                                    }
                                )
                            }
                            actions.add(
                                CustomAccessibilityAction(label = "Sil") {
                                    onDelete()
                                    true
                                }
                            )
                            this.customActions = actions
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (item.isAudio) Icons.Default.Audiotrack else Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = if (item.isAudio) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clearAndSetSemantics { }
                    ) {
                        Text(
                            text = item.name + ringtoneBadge,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.extension} • ${item.formattedSize} • ${item.formattedDate}$resumeBadge",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }

                // Dosyanın hemen yanında erişilebilir "Diğer Seçenekler" butonu
                IconButton(
                    onClick = onOpenOptionsMenu,
                    modifier = Modifier.semantics {
                        this.contentDescription = "${item.name} için diğer seçenekler"
                        this.role = Role.Button
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Görsel Aksiyon Butonları (Gören kullanıcılar için ekranda görünür, TalkBack taramasından gizlidir)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccessibleButton(
                    text = "Oynat",
                    contentDesc = "${item.name} dosyasını oynat",
                    icon = Icons.Default.PlayArrow,
                    containerColor = if (item.isAudio) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    contentColor = if (item.isAudio) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpen
                )

                AccessibleButton(
                    text = "Paylaş",
                    contentDesc = "${item.name} dosyasını paylaş",
                    icon = Icons.Default.Share,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = onShare
                )

                AccessibleButton(
                    text = "Sil",
                    contentDesc = "${item.name} dosyasını sil",
                    icon = Icons.Default.Delete,
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(0.85f),
                    onClick = onDelete
                )
            }
        }
    }
}
