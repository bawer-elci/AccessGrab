package com.baverelci.accessgrab.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.network.ExtractResponse

@Composable
fun QualitySelectorCard(
    media: ExtractResponse,
    selectedFormat: String,
    selectedQuality: String,
    onFormatSelected: (String) -> Unit,
    onQualitySelected: (String) -> Unit,
    onStartDownload: () -> Unit,
    onQuickDownload: ((format: String, quality: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MediaInfoCard(
            media = media,
            onQuickDownload = onQuickDownload,
            onDefaultDownload = onStartDownload
        )

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
                onClick = { onFormatSelected("mp4") }
            )

            val isAudioSelected = selectedFormat == "mp3"
            AccessibleButton(
                text = if (isAudioSelected) "✓ Ses (MP3)" else "Ses (MP3)",
                contentDesc = "Ses formatı ${if (isAudioSelected) "seçili" else "seçmek için dokunun"}",
                icon = Icons.Default.Audiotrack,
                containerColor = if (isAudioSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isAudioSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                onClick = { onFormatSelected("mp3") }
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
                                .clickable { onQualitySelected(qKey) }
                                .semantics {
                                    this.role = Role.RadioButton
                                    this.selected = isSelected
                                    this.contentDescription = "$qLabel, $qDesc. ${if (isSelected) "Seçili" else "Seçmek için çift dokunun"}"
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clearAndSetSemantics { },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onQualitySelected(qKey) }
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
                                .clickable { onQualitySelected(qKey) }
                                .semantics {
                                    this.role = Role.RadioButton
                                    this.selected = isSelected
                                    this.contentDescription = "$qLabel, $qDesc. ${if (isSelected) "Seçili" else "Seçmek için çift dokunun"}"
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clearAndSetSemantics { },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onQualitySelected(qKey) }
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
            onClick = onStartDownload
        )
    }
}
