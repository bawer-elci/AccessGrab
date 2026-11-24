package com.baverelci.accessgrab.player

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.PowerManager
import android.view.SurfaceHolder
import com.baverelci.accessgrab.data.AppLogger
import com.baverelci.accessgrab.data.DownloadedMediaFile
import com.baverelci.accessgrab.data.PreferencesManager
import com.baverelci.accessgrab.notification.PlayerNotificationHelper
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
    val playbackSpeed: Float = 1.0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0
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
    private var activeSurfaceHolder: SurfaceHolder? = null
    private var progressJob: Job? = null
    private var playlist: List<DownloadedMediaFile> = emptyList()
    private var appContext: Context? = null
    private var prefsManager: PreferencesManager? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val availableSpeeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)

    fun attachSurfaceHolder(holder: SurfaceHolder?) {
        activeSurfaceHolder = holder
        try {
            mediaPlayer?.setDisplay(holder)
        } catch (_: Exception) {}
    }

    fun detachSurfaceHolder(holder: SurfaceHolder?) {
        if (activeSurfaceHolder == holder) {
            activeSurfaceHolder = null
            try {
                mediaPlayer?.setDisplay(null)
            } catch (_: Exception) {}
        }
    }

    /**
     * Medyayı başlatır ve kaldığı saniye varsa (forceFromStart = false ise) otomatik olarak oradan devam ettirir.
     * Döndürülen Int: Başlatılan milisaniye konumu (0 ise baştan başlamıştır).
     */
    fun playMedia(
        target: DownloadedMediaFile,
        currentList: List<DownloadedMediaFile> = emptyList(),
        context: Context? = null,
        prefs: PreferencesManager? = null,
        forceFromStart: Boolean = false
    ): Int {
        if (context != null) appContext = context.applicationContext
        if (prefs != null) prefsManager = prefs
        if (currentList.isNotEmpty()) {
            playlist = currentList
        } else if (playlist.isEmpty()) {
            playlist = listOf(target)
        }

        // Mevcut oynatılan dosyanın kaldığı yeri kaydet
        saveCurrentPositionIfApplicable()
        stopInternal()

        val savedPos = if (!forceFromStart) {
            prefsManager?.getSavedPlaybackPosition(target.name) ?: 0
        } else {
            prefsManager?.clearSavedPlaybackPosition(target.name)
            0
        }

        val currentSpeed = _state.value.playbackSpeed
        _state.value = PlayerState(
            currentFile = target,
            isPlaying = false,
            currentPositionMs = savedPos,
            durationMs = 0,
            playbackSpeed = currentSpeed
        )

        try {
            val mp = MediaPlayer().apply {
                appContext?.let { ctx ->
                    try {
                        setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
                    } catch (_: Exception) {}
                }
                setDataSource(target.file.absolutePath)
                activeSurfaceHolder?.let { holder ->
                    try {
                        setDisplay(holder)
                    } catch (_: Exception) {}
                }
                setOnVideoSizeChangedListener { _, width, height ->
                    _state.value = _state.value.copy(
                        videoWidth = width,
                        videoHeight = height
                    )
                }
                setOnPreparedListener { player ->
                    activeSurfaceHolder?.let { holder ->
                        try {
                            player.setDisplay(holder)
                        } catch (_: Exception) {}
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            player.playbackParams = PlaybackParams().setSpeed(currentSpeed)
                        } catch (_: Exception) {}
                    }
                    val totalDur = player.duration.coerceAtLeast(0)
                    val resumeTarget = if (savedPos in 5001 until (totalDur - 3000).coerceAtLeast(5002)) {
                        player.seekTo(savedPos)
                        savedPos
                    } else {
                        0
                    }
                    player.start()
                    _state.value = _state.value.copy(
                        currentFile = target,
                        isPlaying = true,
                        currentPositionMs = resumeTarget,
                        durationMs = totalDur,
                        playbackSpeed = currentSpeed,
                        videoWidth = player.videoWidth,
                        videoHeight = player.videoHeight
                    )
                    updateNotification()
                    startProgressTicker()
                }
                setOnCompletionListener {
                    // Biten dosyanın kaldığı yer kaydını sıfırla ki bir sonraki açılışta baştan başlasın
                    prefsManager?.clearSavedPlaybackPosition(target.name)
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
            AppLogger.log("PLAYER_START", "Oynatıcı başlatıldı: ${target.name} (resumeMs=$savedPos)")
            return savedPos
        } catch (e: Exception) {
            AppLogger.log("PLAYER_ERR", "Medya açılamadı: ${target.name}", e)
            stop()
            return 0
        }
    }

    fun restartFromBeginning() {
        val current = _state.value.currentFile ?: return
        prefsManager?.clearSavedPlaybackPosition(current.name)
        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.seekTo(0)
                if (!mp.isPlaying) mp.start()
                _state.value = _state.value.copy(currentPositionMs = 0, isPlaying = true)
                updateNotification()
            } catch (_: Exception) {}
        }
    }

    fun togglePlayPause(): Boolean {
        val mp = mediaPlayer ?: return false
        return try {
            if (mp.isPlaying) {
                mp.pause()
                val pos = mp.currentPosition.coerceAtLeast(0)
                _state.value = _state.value.copy(
                    isPlaying = false,
                    currentPositionMs = pos
                )
                saveCurrentPositionIfApplicable()
                updateNotification()
                false
            } else {
                mp.start()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        mp.playbackParams = PlaybackParams().setSpeed(_state.value.playbackSpeed)
                    } catch (_: Exception) {}
                }
                _state.value = _state.value.copy(isPlaying = true)
                updateNotification()
                startProgressTicker()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun seekBy(deltaMs: Int) {
        val mp = mediaPlayer ?: return
        try {
            val dur = mp.duration.coerceAtLeast(0)
            val newPos = (mp.currentPosition + deltaMs).coerceIn(0, dur)
            mp.seekTo(newPos)
            _state.value = _state.value.copy(currentPositionMs = newPos)
            saveCurrentPositionIfApplicable()
            updateNotification()
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
            playMedia(playlist[idx + 1], playlist, appContext, prefsManager)
            return true
        }
        return false
    }

    fun playPrevious(): Boolean {
        val currentPath = _state.value.currentFile?.file?.absolutePath ?: return false
        val idx = playlist.indexOfFirst { it.file.absolutePath == currentPath }
        if (idx > 0) {
            playMedia(playlist[idx - 1], playlist, appContext, prefsManager)
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
        saveCurrentPositionIfApplicable()
        stopInternal()
        PlayerNotificationHelper.cancelNotification(appContext)
        _state.value = PlayerState()
    }

    private fun saveCurrentPositionIfApplicable() {
        val current = _state.value.currentFile ?: return
        val pos = _state.value.currentPositionMs
        val dur = _state.value.durationMs
        if (pos > 5000 && (dur <= 0 || pos < dur - 3000)) {
            prefsManager?.savePlaybackPosition(current.name, pos)
        } else if (dur > 0 && pos >= dur - 3000) {
            prefsManager?.clearSavedPlaybackPosition(current.name)
        }
    }

    private fun updateNotification() {
        val ctx = appContext ?: return
        val s = _state.value
        val file = s.currentFile ?: return
        PlayerNotificationHelper.showOrUpdateNotification(
            context = ctx,
            title = file.name,
            isPlaying = s.isPlaying,
            positionText = s.formattedPosition,
            durationText = s.formattedDuration
        )
    }

    private fun stopInternal() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.setDisplay(null)
        } catch (_: Exception) {}
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
            var tickCount = 0
            while (true) {
                val mp = mediaPlayer ?: break
                try {
                    if (mp.isPlaying) {
                        _state.value = _state.value.copy(
                            isPlaying = true,
                            currentPositionMs = mp.currentPosition.coerceAtLeast(0),
                            durationMs = mp.duration.coerceAtLeast(0)
                        )
                        tickCount++
                        if (tickCount % 5 == 0) {
                            saveCurrentPositionIfApplicable()
                            updateNotification()
                        }
                    }
                } catch (_: Exception) {}
                delay(400)
            }
        }
    }
}
