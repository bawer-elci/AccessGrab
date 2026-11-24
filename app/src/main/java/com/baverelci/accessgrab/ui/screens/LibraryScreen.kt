package com.baverelci.accessgrab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.accessibility.AccessibilityHelper
import com.baverelci.accessgrab.data.DownloadedMediaFile
import com.baverelci.accessgrab.data.MediaFileManager
import com.baverelci.accessgrab.ui.components.AccessibleButton
import com.baverelci.accessgrab.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isHaptic by viewModel.prefs.isHapticEnabled.collectAsState()

    var files by remember { mutableStateOf<List<DownloadedMediaFile>>(emptyList()) }
    var fileToDelete by remember { mutableStateOf<DownloadedMediaFile?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    fun refreshFiles() {
        files = MediaFileManager.getDownloadedFiles()
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            AccessibilityHelper.announce(view, msg)
            feedbackMessage = null
        }
    }

    // Silme Onay Diyaloğu
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
                    contentDesc = "${target.name} dosyasını kalıcı olarak sil düğmesi",
                    icon = Icons.Default.Delete,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = {
                        val deleted = MediaFileManager.deleteMediaFile(target.file)
                        if (deleted) {
                            if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                            feedbackMessage = "${target.name} dosyası başarıyla silindi."
                            refreshFiles()
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Başlık ve Yenile Butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kütüphanem",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = if (files.isEmpty()) "Henüz indirilmiş medya yok" else "${files.size} adet medya dosyası",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            AccessibleButton(
                text = "Yenile",
                contentDesc = "İndirilen dosyalar listesini yenile",
                icon = Icons.Default.Refresh,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    refreshFiles()
                    if (isHaptic) AccessibilityHelper.vibrateSuccess(context)
                    feedbackMessage = "Dosya listesi güncellendi. Toplam ${files.size} dosya var."
                }
            )
        }

        if (files.isEmpty()) {
            // Boş Durum Kartı
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        this.contentDescription = "Kütüphaneniz şu an boş. İndir sekmesinden indirdiğiniz tüm video ve sesler burada listelenecektir."
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
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
                        text = "İndir sekmesinden indirdiğiniz tüm MP4 videolar ve MP3 sesler telefonunuzun AccessGrab klasörüne kaydedilir ve burada erişilebilir olarak listelenir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            // İndirilen Dosyalar Listesi
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(files, key = { it.file.absolutePath }) { item ->
                    MediaFileItemCard(
                        item = item,
                        onOpen = {
                            val result = MediaFileManager.openMediaFile(context, item.file)
                            if (result.isFailure) {
                                if (isHaptic) AccessibilityHelper.vibrateError(context)
                                feedbackMessage = "Dosya açılamadı: Uygun bir oynatıcı bulunamadı."
                            }
                        },
                        onShare = {
                            val result = MediaFileManager.shareMediaFile(context, item.file)
                            if (result.isFailure) {
                                if (isHaptic) AccessibilityHelper.vibrateError(context)
                                feedbackMessage = "Paylaşım başlatılamadı."
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
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibleDesc = "${item.extension} dosyası: ${item.name}. Boyut: ${item.formattedSize}. Tarih: ${item.formattedDate}."

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.contentDescription = accessibleDesc
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (item.isAudio) Icons.Default.Audiotrack else Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = if (item.isAudio) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.extension} • ${item.formattedSize} • ${item.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Aksiyon Butonları (Oynat, Paylaş, Sil)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccessibleButton(
                    text = "Aç",
                    contentDesc = "${item.name} dosyasını aç ve oynat",
                    icon = Icons.Default.PlayArrow,
                    containerColor = if (item.isAudio) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    contentColor = if (item.isAudio) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpen
                )

                AccessibleButton(
                    text = "Paylaş",
                    contentDesc = "${item.name} dosyasını diğer uygulamalarla paylaş",
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
                    modifier = Modifier.weight(0.7f),
                    onClick = onDelete
                )
            }
        }
    }
}
