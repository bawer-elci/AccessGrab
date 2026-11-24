package com.baverelci.accessgrab.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

fun formatDownloadBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun DownloadProgressCard(
    title: String,
    format: String,
    progress: Int,
    downloadedBytes: Long,
    totalBytes: Long,
    queueCount: Int = 0,
    queuedTitles: List<String> = emptyList(),
    onCancelDownload: (() -> Unit)? = null,
    onCancelAllQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPreparing = totalBytes <= 0L && downloadedBytes <= 0L
    val queueSuffix = if (queueCount > 0) " Sırada bekleyen $queueCount medya var." else ""
    val statusDesc = if (isPreparing) {
        "$title sunucuda hazırlanıyor, lütfen bekleyin.$queueSuffix"
    } else {
        "$title indiriliyor. İlerleme: Yüzde $progress.$queueSuffix"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                this.liveRegion = LiveRegionMode.Polite
                this.contentDescription = statusDesc
                val actions = mutableListOf<CustomAccessibilityAction>()
                if (onCancelDownload != null) {
                    actions.add(
                        CustomAccessibilityAction("İndirmeyi İptal Et") {
                            onCancelDownload()
                            true
                        }
                    )
                }
                if (queueCount > 0 && onCancelAllQueue != null) {
                    actions.add(
                        CustomAccessibilityAction("Tüm Kuyruğu İptal Et ($queueCount bekleyen)") {
                            onCancelAllQueue()
                            true
                        }
                    )
                }
                if (actions.isNotEmpty()) {
                    this.customActions = actions
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clearAndSetSemantics { },
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
                        text = if (isPreparing) "Sunucuda Hazırlanıyor ($format)…" else "İndiriliyor ($format)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!isPreparing) {
                    Text(
                        text = "%$progress",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            // Canlı İlerleme Çubuğu
            if (totalBytes > 0) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
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
                        text = formatDownloadBytes(downloadedBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatDownloadBytes(totalBytes),
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

            // İndirme Kuyruğu Bilgisi (Varsa)
            if (queueCount > 0) {
                Text(
                    text = "Sırada Bekleyen ($queueCount): ${queuedTitles.take(2).joinToString(", ")}${if (queueCount > 2) "…" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // İndirmeyi İptal Et Butonu
            if (onCancelDownload != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccessibleButton(
                        text = if (queueCount > 0) "Bunu Geç / İptal" else "İndirmeyi İptal Et",
                        contentDesc = "Mevcut indirmeyi iptal et",
                        icon = Icons.Default.Close,
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = onCancelDownload
                    )

                    if (queueCount > 0 && onCancelAllQueue != null) {
                        AccessibleButton(
                            text = "Tümünü Durdur",
                            contentDesc = "Tüm indirme kuyruğunu iptal et",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            onClick = onCancelAllQueue
                        )
                    }
                }
            }
        }
    }
}
