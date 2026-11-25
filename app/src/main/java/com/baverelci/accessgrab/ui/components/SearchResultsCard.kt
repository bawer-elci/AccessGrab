package com.baverelci.accessgrab.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.network.SearchResultItem

@Composable
fun SearchResultsCard(
    query: String,
    results: List<SearchResultItem>,
    canLoadMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    onPreviewListen: ((SearchResultItem) -> Unit)? = null,
    onSelectResultForQuality: (SearchResultItem) -> Unit,
    onDirectDownload: (item: SearchResultItem, format: String, quality: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMenuResult by remember { mutableStateOf<SearchResultItem?>(null) }

    if (selectedMenuResult != null) {
        val target = selectedMenuResult!!
        AlertDialog(
            onDismissRequest = { selectedMenuResult = null },
            title = {
                Text(
                    text = target.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${target.author} • Süre: ${target.durationFormatted}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    if (onPreviewListen != null) {
                        AccessibleButton(
                            text = "İndirmeden Önce Önizle / Dinle",
                            contentDesc = "${target.title} medyasını telefona indirmeden önce canlı dinle",
                            icon = Icons.Default.Headphones,
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = {
                                val item = target
                                selectedMenuResult = null
                                onPreviewListen(item)
                            }
                        )
                    }
                    AccessibleButton(
                        text = "MP3 Ses İndir (192 kbps)",
                        contentDesc = "${target.title} medyasını 192 kbps MP3 ses olarak indir",
                        icon = Icons.Default.Audiotrack,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        onClick = {
                            val item = target
                            selectedMenuResult = null
                            onDirectDownload(item, "mp3", "192")
                        }
                    )
                    AccessibleButton(
                        text = "MP3 Ses İndir (320 kbps Yüksek)",
                        contentDesc = "${target.title} medyasını 320 kbps yüksek kalite MP3 ses olarak indir",
                        icon = Icons.Default.Audiotrack,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            val item = target
                            selectedMenuResult = null
                            onDirectDownload(item, "mp3", "320")
                        }
                    )
                    AccessibleButton(
                        text = "MP4 Video İndir (720p HD)",
                        contentDesc = "${target.title} medyasını 720p HD MP4 video olarak indir",
                        icon = Icons.Default.VideoLibrary,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = {
                            val item = target
                            selectedMenuResult = null
                            onDirectDownload(item, "mp4", "720")
                        }
                    )
                    AccessibleButton(
                        text = "MP4 Video İndir (1080p Tam HD)",
                        contentDesc = "${target.title} medyasını 1080p Tam HD MP4 video olarak indir",
                        icon = Icons.Default.VideoLibrary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            val item = target
                            selectedMenuResult = null
                            onDirectDownload(item, "mp4", "1080")
                        }
                    )
                    AccessibleButton(
                        text = "Detaylı Kalite Seçimi Aç",
                        contentDesc = "Tüm çözünürlük ve kalite seçeneklerini ekranda aç",
                        icon = Icons.Default.Tune,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            val item = target
                            selectedMenuResult = null
                            onSelectResultForQuality(item)
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
                    onClick = { selectedMenuResult = null }
                )
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "\"$query\" Arama Sonuçları (${results.size})",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )

        results.forEach { item ->
            val summary = "${item.title}. Kanal: ${item.author}. Süre: ${item.durationFormatted}."
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectResultForQuality(item) }
                            .semantics {
                                this.contentDescription = summary
                                this.role = Role.Button
                                val actions = mutableListOf<CustomAccessibilityAction>()
                                if (onPreviewListen != null) {
                                    actions.add(
                                        CustomAccessibilityAction("İndirmeden Önce Önizle / Dinle") {
                                            onPreviewListen(item)
                                            true
                                        }
                                    )
                                }
                                actions.add(
                                    CustomAccessibilityAction("MP3 Ses İndir (192 kbps)") {
                                        onDirectDownload(item, "mp3", "192")
                                        true
                                    }
                                )
                                actions.add(
                                    CustomAccessibilityAction("MP3 Ses İndir (320 kbps Yüksek)") {
                                        onDirectDownload(item, "mp3", "320")
                                        true
                                    }
                                )
                                actions.add(
                                    CustomAccessibilityAction("MP4 Video İndir (720p HD)") {
                                        onDirectDownload(item, "mp4", "720")
                                        true
                                    }
                                )
                                actions.add(
                                    CustomAccessibilityAction("MP4 Video İndir (1080p Tam HD)") {
                                        onDirectDownload(item, "mp4", "1080")
                                        true
                                    }
                                )
                                actions.add(
                                    CustomAccessibilityAction("Diğer Seçenekler Menüsü") {
                                        selectedMenuResult = item
                                        true
                                    }
                                )
                                actions.add(
                                    CustomAccessibilityAction("Kalite Seçimi Ekranını Aç") {
                                        onSelectResultForQuality(item)
                                        true
                                    }
                                )
                                this.customActions = actions
                            }
                    ) {
                        Column(modifier = Modifier.clearAndSetSemantics { }) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.author} • ${item.durationFormatted}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { selectedMenuResult = item },
                        modifier = Modifier.semantics {
                            this.contentDescription = "${item.title} için diğer seçenekler"
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
            }
        }

        // #17 — Daha Fazla Sonuç Getir Butonu
        if (canLoadMore && onLoadMore != null) {
            AccessibleButton(
                text = if (isLoadingMore) "Daha Fazla Sonuç Getiriliyor…" else "Daha Fazla Sonuç Getir",
                contentDesc = "Bu arama için daha fazla sonuç yükle",
                icon = Icons.Default.Add,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                enabled = !isLoadingMore,
                onClick = onLoadMore
            )
        }
    }
}
