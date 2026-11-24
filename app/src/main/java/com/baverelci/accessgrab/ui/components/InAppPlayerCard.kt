package com.baverelci.accessgrab.ui.components

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.baverelci.accessgrab.player.InAppPlayerManager
import com.baverelci.accessgrab.player.PlayerState

@Composable
fun InAppPlayerCard(
    playerState: PlayerState,
    onTogglePlayPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onRestartFromBeginning: (() -> Unit)? = null,
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

    val videoRatio = if (playerState.videoWidth > 0 && playerState.videoHeight > 0) {
        (playerState.videoWidth.toFloat() / playerState.videoHeight.toFloat()).coerceIn(0.56f, 2.2f)
    } else {
        16f / 9f
    }

    val headerDesc = "${if (file.isAudio) "Ses Oynatıcı Penceresi" else "Video Oynatıcı Penceresi"} ($statusText): ${file.name}. " +
        "Geçen süre: ${playerState.formattedPosition}, Toplam süre: ${playerState.formattedDuration}. " +
        "Oynatma hızı: ${playerState.formattedSpeed}."

    Dialog(
        onDismissRequest = onStop,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Başlık ve Durum Özeti (TalkBack Eylemler menüsü de bu başlığa bağlıdır)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTogglePlayPause)
                        .semantics {
                            this.heading()
                            this.role = Role.Button
                            this.contentDescription = headerDesc
                            this.customActions = listOf(
                                CustomAccessibilityAction(if (playerState.isPlaying) "Duraklat" else "Oynat") {
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
                                CustomAccessibilityAction("Hızlandır (${playerState.formattedSpeed})") {
                                    onCycleSpeed()
                                    true
                                },
                                CustomAccessibilityAction("Baştan Başlat") {
                                    onRestartFromBeginning?.invoke()
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
                                CustomAccessibilityAction("Pencereyi Kapat") {
                                    onStop()
                                    true
                                }
                            )
                        }
                ) {
                    Column(modifier = Modifier.clearAndSetSemantics { }) {
                        Text(
                            text = if (file.isAudio) "MP3 Ses Oynatıcı • $statusText" else "MP4 Video Oynatıcı • $statusText",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                    }
                }

                // 2. Görüntü Alanı (MP4 ise gerçek video görüntüsü SurfaceView üzerinde oynar, MP3 ise ses kartı görünür)
                if (!file.isAudio) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(videoRatio)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                            .clickable(onClick = onTogglePlayPause)
                            .clearAndSetSemantics { },
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceView(ctx).apply {
                                    keepScreenOn = true
                                    holder.addCallback(object : SurfaceHolder.Callback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            InAppPlayerManager.attachSurfaceHolder(holder)
                                        }

                                        override fun surfaceChanged(
                                            holder: SurfaceHolder,
                                            format: Int,
                                            width: Int,
                                            height: Int
                                        ) {
                                            InAppPlayerManager.attachSurfaceHolder(holder)
                                        }

                                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                                            InAppPlayerManager.detachSurfaceHolder(holder)
                                        }
                                    })
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                            .clearAndSetSemantics { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                // 3. Süre Çubuğu ve Dakika/Saniye Bilgisi
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { }
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = playerState.formattedPosition,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hız: ${playerState.formattedSpeed}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = playerState.formattedDuration,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 4. Erişilebilir Büyük Oynatıcı Düğmeleri (Duraklat/Oynat, Geri Sar, İleri Sar, Hızlandır)
                AccessibleButton(
                    text = if (playerState.isPlaying) "Duraklat" else "Oynat",
                    contentDesc = if (playerState.isPlaying) "Medyayı duraklat (${playerState.formattedPosition})" else "Medyayı oynatmaya devam et",
                    icon = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onTogglePlayPause
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccessibleButton(
                        text = "10 Sn Geri",
                        contentDesc = "10 saniye geri sar",
                        icon = Icons.Default.Replay10,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        onClick = onSeekBackward
                    )

                    AccessibleButton(
                        text = "10 Sn İleri",
                        contentDesc = "10 saniye ileri sar",
                        icon = Icons.Default.Forward10,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        onClick = onSeekForward
                    )
                }

                // Hızlandır ve Baştan Başlat Satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccessibleButton(
                        text = "Hız (${playerState.formattedSpeed})",
                        contentDesc = "Oynatma hızını değiştir. Şu anki hız: ${playerState.formattedSpeed}",
                        icon = Icons.Default.Speed,
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onCycleSpeed
                    )

                    if (onRestartFromBeginning != null) {
                        AccessibleButton(
                            text = "Baştan",
                            contentDesc = "Medyayı en baştan başlat",
                            icon = Icons.Default.Replay,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            onClick = onRestartFromBeginning
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccessibleButton(
                        text = "Önceki",
                        contentDesc = "Önceki medyaya geç",
                        icon = Icons.Default.SkipPrevious,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        onClick = onPrevious
                    )

                    AccessibleButton(
                        text = "Sonraki",
                        contentDesc = "Sonraki medyaya geç",
                        icon = Icons.Default.SkipNext,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        onClick = onNext
                    )
                }

                // Pencereyi Kapat Düğmesi
                AccessibleButton(
                    text = "Pencereyi Kapat",
                    contentDesc = "Oynatıcı penceresini kapat ve oynatmayı durdur",
                    icon = Icons.Default.Close,
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = onStop
                )
            }
        }
    }
}
