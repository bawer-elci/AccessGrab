package com.baverelci.accessgrab.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.baverelci.accessgrab.network.ExtractResponse

@Composable
fun MediaInfoCard(
    media: ExtractResponse,
    onQuickDownload: ((format: String, quality: String) -> Unit)? = null,
    onDefaultDownload: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accessibleSummary = "${media.platform} medyası: ${media.title}. Yükleyen: ${media.author}. Süre: ${media.durationFormatted}."

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onDefaultDownload != null) Modifier.clickable(onClick = onDefaultDownload)
                else Modifier
            )
            .semantics {
                this.contentDescription = accessibleSummary
                if (onDefaultDownload != null) {
                    this.role = Role.Button
                }
                if (onQuickDownload != null) {
                    this.customActions = listOf(
                        CustomAccessibilityAction("MP4 Video İndir (720p Standart HD)") {
                            onQuickDownload("mp4", "720")
                            true
                        },
                        CustomAccessibilityAction("MP4 Video İndir (1080p Tam HD)") {
                            onQuickDownload("mp4", "1080")
                            true
                        },
                        CustomAccessibilityAction("MP4 Video İndir (480p SD)") {
                            onQuickDownload("mp4", "480")
                            true
                        },
                        CustomAccessibilityAction("MP4 Video İndir (360p Düşük Boyut)") {
                            onQuickDownload("mp4", "360")
                            true
                        },
                        CustomAccessibilityAction("MP3 Ses İndir (192 kbps Standart)") {
                            onQuickDownload("mp3", "192")
                            true
                        },
                        CustomAccessibilityAction("MP3 Ses İndir (320 kbps Yüksek Kalite)") {
                            onQuickDownload("mp3", "320")
                            true
                        },
                        CustomAccessibilityAction("MP3 Ses İndir (128 kbps Tasarruf)") {
                            onQuickDownload("mp3", "128")
                            true
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clearAndSetSemantics { }
        ) {
            // Başlık
            Text(
                text = media.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Küçük Resim ve Detaylar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (media.thumbnail.isNotBlank()) {
                    AsyncImage(
                        model = media.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Platform
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = media.platform,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Yükleyen
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = media.author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Süre
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = media.durationFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
