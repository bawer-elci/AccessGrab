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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.player.PlayerState

@Composable
fun InAppPlayerCard(
    playerState: PlayerState,
    onTogglePlayPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCycleSpeed: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val file = playerState.currentFile ?: return
    val statusText = if (playerState.isPlaying) "Oynatılıyor" else "Duraklatıldı"
    val progressFraction = if (playerState.durationMs > 0) {
        (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val cardDesc = "Uygulama İçi Oynatıcı ($statusText): ${file.name}. " +
        "Süre: ${playerState.formattedPosition} / ${playerState.formattedDuration}. " +
        "Hız: ${playerState.formattedSpeed}."

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTogglePlayPause)
            .semantics {
                this.contentDescription = cardDesc
                this.role = Role.Button
                this.customActions = listOf(
                    CustomAccessibilityAction(if (playerState.isPlaying) "Duraklat" else "Devam Et") {
                        onTogglePlayPause()
                        true
                    },
                    CustomAccessibilityAction("10 Saniye İleri Sar") {
                        onSeekForward()
                        true
                    },
                    CustomAccessibilityAction("10 Saniye Geri Sar") {
                        onSeekBackward()
                        true
                    },
                    CustomAccessibilityAction("Oynatma Hızını Değiştir (${playerState.formattedSpeed})") {
                        onCycleSpeed()
                        true
                    },
                    CustomAccessibilityAction("Sonraki Medyaya Geç") {
                        onNext()
                        true
                    },
                    CustomAccessibilityAction("Önceki Medyaya Geç") {
                        onPrevious()
                        true
                    },
                    CustomAccessibilityAction("Oynatıcıyı Kapat") {
                        onStop()
                        true
                    }
                )
            }
    ) {
        // Görsel kontroller gören kullanıcılar için ekranda kalır; TalkBack Eylemler menüsüyle yönetilir.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$statusText • ${playerState.formattedSpeed}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = playerState.formattedPosition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Text(
                    text = playerState.formattedDuration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSeekBackward) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onSeekForward) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onCycleSpeed)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = playerState.formattedSpeed,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
