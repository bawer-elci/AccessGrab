package com.baverelci.accessgrab.player

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import com.baverelci.accessgrab.data.AppLogger
import com.baverelci.accessgrab.data.DownloadedMediaFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerState(
    val currentFile: DownloadedMediaFile? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f
) {
    val isActive: Boolean get() = currentFile != null
    val formattedPosition: String get() = formatTimeMs(currentPositionMs)
    val formattedDuration: String get() = formatTimeMs(durationMs)
    val formattedSpeed: String get() = if (playbackSpeed % 1.0f == 0f) "${playbackSpeed.toInt()}x" else "${playbackSpeed}x"
}

private fun formatTimeMs(ms: Int): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}

object InAppPlayerManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var playlist: List<DownloadedMediaFile> = emptyList()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val availableSpeeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)

    fun playMedia(target: DownloadedMediaFile, currentList: List<DownloadedMediaFile> = emptyList()) {
        if (currentList.isNotEmpty()) {
            playlist = currentList
        } else if (playlist.isEmpty()) {
            playlist = listOf(target)
        }

        // Aynı dosya zaten yüklüyse oynat/duraklat geçişi yap
        if (_state.value.currentFile?.file?.absolutePath == target.file.absolutePath && mediaPlayer != null) {
            togglePlayPause()
            return
        }

        stopInternal()

        try {
            val currentSpeed = _state.value.playbackSpeed
            val mp = MediaPlayer().apply {
                setDataSource(target.file.absolutePath)
                setOnPreparedListener { player ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            player.playbackParams = PlaybackParams().setSpeed(currentSpeed)
                        } catch (_: Exception) {}
                    }
                    player.start()
                    _state.value = PlayerState(
                        currentFile = target,
                        isPlaying = true,
                        currentPositionMs = 0,
                        durationMs = player.duration.coerceAtLeast(0),
                        playbackSpeed = currentSpeed
                    )
                    startProgressTicker()
                }
                setOnCompletionListener {
                    playNextOrStop()
                }
                setOnErrorListener { _, what, extra ->
                    AppLogger.log("PLAYER_ERR", "MediaPlayer hatası: what=$what, extra=$extra (${target.name})")
                    stop()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
            AppLogger.log("PLAYER_START", "Uygulama içi oynatıcı başlatıldı: ${target.name}")
        } catch (e: Exception) {
            AppLogger.log("PLAYER_ERR", "Medya açılamadı: ${target.name}", e)
            stop()
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _state.value = _state.value.copy(
                    isPlaying = false,
                    currentPositionMs = mp.currentPosition.coerceAtLeast(0)
                )
            } else {
                mp.start()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        mp.playbackParams = PlaybackParams().setSpeed(_state.value.playbackSpeed)
                    } catch (_: Exception) {}
                }
                _state.value = _state.value.copy(isPlaying = true)
                startProgressTicker()
            }
        } catch (_: Exception) {}
    }

    fun seekBy(deltaMs: Int) {
        val mp = mediaPlayer ?: return
        try {
            val dur = mp.duration.coerceAtLeast(0)
            val newPos = (mp.currentPosition + deltaMs).coerceIn(0, dur)
            mp.seekTo(newPos)
            _state.value = _state.value.copy(currentPositionMs = newPos)
        } catch (_: Exception) {}
    }

    fun cyclePlaybackSpeed(): Float {
        val current = _state.value.playbackSpeed
        val idx = availableSpeeds.indexOfFirst { it == current }
        val nextSpeed = availableSpeeds[(idx + 1) % availableSpeeds.size]
        _state.value = _state.value.copy(playbackSpeed = nextSpeed)

        val mp = mediaPlayer
        if (mp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val wasPlaying = mp.isPlaying
                mp.playbackParams = PlaybackParams().setSpeed(nextSpeed)
                if (!wasPlaying) {
                    mp.pause()
                }
            } catch (_: Exception) {}
        }
        return nextSpeed
    }

    fun playNext(): Boolean {
        val currentPath = _state.value.currentFile?.file?.absolutePath ?: return false
        val idx = playlist.indexOfFirst { it.file.absolutePath == currentPath }
        if (idx >= 0 && idx + 1 < playlist.size) {
            playMedia(playlist[idx + 1], playlist)
            return true
        }
        return false
    }

    fun playPrevious(): Boolean {
        val currentPath = _state.value.currentFile?.file?.absolutePath ?: return false
        val idx = playlist.indexOfFirst { it.file.absolutePath == currentPath }
        if (idx > 0) {
            playMedia(playlist[idx - 1], playlist)
            return true
        }
        return false
    }

    private fun playNextOrStop() {
        if (!playNext()) {
            stop()
        }
    }

    fun stop() {
        stopInternal()
        _state.value = PlayerState()
    }

    private fun stopInternal() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val mp = mediaPlayer ?: break
                try {
                    if (mp.isPlaying) {
                        _state.value = _state.value.copy(
                            isPlaying = true,
                            currentPositionMs = mp.currentPosition.coerceAtLeast(0),
                            durationMs = mp.duration.coerceAtLeast(0)
                        )
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }
    }
}
