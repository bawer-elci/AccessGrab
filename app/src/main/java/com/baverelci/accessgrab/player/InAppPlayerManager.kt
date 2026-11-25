package com.baverelci.accessgrab.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.LoudnessEnhancer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.PowerManager
import android.view.SurfaceHolder
import com.baverelci.accessgrab.data.AppLogger
import com.baverelci.accessgrab.data.DownloadedMediaFile
import com.baverelci.accessgrab.data.PreferencesManager
import com.baverelci.accessgrab.network.AccessGrabApi
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
import java.io.File

data class PlayerState(
    val currentFile: DownloadedMediaFile? = null,
    val isRemotePreview: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val volumeBoostPercent: Int = 100,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0
) {
    val isActive: Boolean get() = currentFile != null
    val formattedPosition: String get() = formatTimeMs(currentPositionMs)
    val formattedDuration: String get() = formatTimeMs(durationMs)
    val formattedSpeed: String get() = if (playbackSpeed % 1.0f == 0f) "${playbackSpeed.toInt()}x" else "${playbackSpeed}x"
    val formattedBoost: String get() = when (volumeBoostPercent) {
        150 -> "%150 Yüksek Ses"
        200 -> "%200 Maksimum Ses"
        else -> "%100 Normal Ses"
    }
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
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pausedByAudioFocus: Boolean = false

    private var activeSurfaceHolder: SurfaceHolder? = null
    private var progressJob: Job? = null
    private var playlist: List<DownloadedMediaFile> = emptyList()
    private var appContext: Context? = null
    private var prefsManager: PreferencesManager? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val availableSpeeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
    private val availableBoosts = listOf(100, 150, 200)

    // #49 — Telefon araması veya sesli mesaj geldiğinde otomatik duraklatıp görüşme bitince devam ettiren AudioFocus dinleyicisi
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_state.value.isPlaying) {
                    pausedByAudioFocus = true
                    pauseInternal()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (pausedByAudioFocus) {
                    pausedByAudioFocus = false
                    resumeInternal()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedByAudioFocus = false
                pauseInternal()
            }
        }
    }

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
     * #8 — Arama sonuçlarında veya incelenen medyada dosyayı telefona indirmeden önce canlı önizleme / dinleme başlatır.
     */
    fun playRemotePreview(
        title: String,
        mediaUrl: String,
        context: Context
    ) {
        appContext = context.applicationContext
        saveCurrentPositionIfApplicable()
        stopInternal()

        val streamUrl = AccessGrabApi.buildPreviewUrl(mediaUrl)
        val previewItem = DownloadedMediaFile(
            file = File(context.cacheDir, "Canli_Onizleme.mp3"),
            name = title,
            sizeBytes = 0L,
            formattedSize = "Canlı Önizleme",
            lastModified = System.currentTimeMillis(),
            formattedDate = "İndirmeden Dinle",
            isAudio = true,
            extension = "CANLI"
        )

        val currentSpeed = _state.value.playbackSpeed
        val currentBoost = _state.value.volumeBoostPercent

        _state.value = PlayerState(
            currentFile = previewItem,
            isRemotePreview = true,
            isBuffering = true,
            isPlaying = false,
            currentPositionMs = 0,
            durationMs = 0,
            playbackSpeed = currentSpeed,
            volumeBoostPercent = currentBoost
        )

        requestAudioFocus()
        ensureMediaSession(title)

        try {
            val mp = MediaPlayer().apply {
                try {
                    setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (_: Exception) {}
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(streamUrl)
                setOnPreparedListener { player ->
                    applyBoostToPlayer(player, _state.value.volumeBoostPercent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            player.playbackParams = PlaybackParams().setSpeed(_state.value.playbackSpeed)
                        } catch (_: Exception) {}
                    }
                    player.start()
                    _state.value = _state.value.copy(
                        isBuffering = false,
                        isPlaying = true,
                        durationMs = player.duration.coerceAtLeast(0)
                    )
                    updateMediaSessionState()
                    updateNotification()
                    startProgressTicker()
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    AppLogger.log("PREVIEW_ERR", "Canlı önizleme hatası: what=$what, extra=$extra ($title)")
                    stop()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
            AppLogger.log("PREVIEW_START", "Canlı önizleme başlatıldı: $title")
        } catch (e: Exception) {
            AppLogger.log("PREVIEW_ERR", "Canlı önizleme açılamadı: $title", e)
            stop()
        }
    }

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

        saveCurrentPositionIfApplicable()
        stopInternal()

        val savedPos = if (!forceFromStart) {
            prefsManager?.getSavedPlaybackPosition(target.name) ?: 0
        } else {
            prefsManager?.clearSavedPlaybackPosition(target.name)
            0
        }

        val currentSpeed = _state.value.playbackSpeed
        val currentBoost = _state.value.volumeBoostPercent

        _state.value = PlayerState(
            currentFile = target,
            isRemotePreview = false,
            isBuffering = false,
            isPlaying = false,
            currentPositionMs = savedPos,
            durationMs = 0,
            playbackSpeed = currentSpeed,
            volumeBoostPercent = currentBoost
        )

        requestAudioFocus()
        ensureMediaSession(target.name)

        try {
            val mp = MediaPlayer().apply {
                appContext?.let { ctx ->
                    try {
                        setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
                    } catch (_: Exception) {}
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(if (target.isAudio) AudioAttributes.CONTENT_TYPE_MUSIC else AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
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
                    applyBoostToPlayer(player, _state.value.volumeBoostPercent)
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
                    updateMediaSessionState()
                    updateNotification()
                    startProgressTicker()
                }
                setOnCompletionListener {
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

    /**
     * #5 — %200 Ses Güçlendirici (LoudnessEnhancer: %100 Normal -> %150 Yüksek -> %200 Maksimum)
     */
    fun cycleVolumeBoost(): Int {
        val current = _state.value.volumeBoostPercent
        val idx = availableBoosts.indexOfFirst { it == current }
        val nextBoost = availableBoosts[(idx + 1) % availableBoosts.size]
        _state.value = _state.value.copy(volumeBoostPercent = nextBoost)
        mediaPlayer?.let { applyBoostToPlayer(it, nextBoost) }
        return nextBoost
    }

    private fun applyBoostToPlayer(player: MediaPlayer, boostPercent: Int) {
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            if (boostPercent > 100) {
                val gainMb = if (boostPercent >= 200) 3000 else 1500
                loudnessEnhancer = LoudnessEnhancer(player.audioSessionId).apply {
                    setTargetGain(gainMb)
                    enabled = true
                }
            }
        } catch (_: Exception) {}
    }

    fun restartFromBeginning() {
        val current = _state.value.currentFile ?: return
        if (!_state.value.isRemotePreview) {
            prefsManager?.clearSavedPlaybackPosition(current.name)
        }
        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.seekTo(0)
                if (!mp.isPlaying) mp.start()
                _state.value = _state.value.copy(currentPositionMs = 0, isPlaying = true)
                updateMediaSessionState()
                updateNotification()
            } catch (_: Exception) {}
        }
    }

    fun togglePlayPause(): Boolean {
        val mp = mediaPlayer ?: return false
        return if (mp.isPlaying) {
            pausedByAudioFocus = false
            pauseInternal()
            false
        } else {
            requestAudioFocus()
            resumeInternal()
            true
        }
    }

    private fun pauseInternal() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) mp.pause()
            val pos = mp.currentPosition.coerceAtLeast(0)
            _state.value = _state.value.copy(
                isPlaying = false,
                currentPositionMs = pos
            )
            saveCurrentPositionIfApplicable()
            updateMediaSessionState()
            updateNotification()
        } catch (_: Exception) {}
    }

    private fun resumeInternal() {
        val mp = mediaPlayer ?: return
        try {
            mp.start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mp.playbackParams = PlaybackParams().setSpeed(_state.value.playbackSpeed)
                } catch (_: Exception) {}
            }
            _state.value = _state.value.copy(isPlaying = true)
            updateMediaSessionState()
            updateNotification()
            startProgressTicker()
        } catch (_: Exception) {}
    }

    fun seekBy(deltaMs: Int) {
        val mp = mediaPlayer ?: return
        try {
            val dur = mp.duration.coerceAtLeast(0)
            val newPos = (mp.currentPosition + deltaMs).coerceIn(0, dur)
            mp.seekTo(newPos)
            _state.value = _state.value.copy(currentPositionMs = newPos)
            saveCurrentPositionIfApplicable()
            updateMediaSessionState()
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
        updateMediaSessionState()
        return nextSpeed
    }

    fun playNext(): Boolean {
        if (_state.value.isRemotePreview) return false
        val currentPath = _state.value.currentFile?.file?.absolutePath ?: return false
        val idx = playlist.indexOfFirst { it.file.absolutePath == currentPath }
        if (idx >= 0 && idx + 1 < playlist.size) {
            playMedia(playlist[idx + 1], playlist, appContext, prefsManager)
            return true
        }
        return false
    }

    fun playPrevious(): Boolean {
        if (_state.value.isRemotePreview) return false
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
        pausedByAudioFocus = false
        saveCurrentPositionIfApplicable()
        stopInternal()
        abandonAudioFocus()
        releaseMediaSession()
        PlayerNotificationHelper.cancelNotification(appContext)
        _state.value = PlayerState()
    }

    /**
     * #26 — TalkBack "İki Parmakla Çift Dokunma" ve Bluetooth Kulaklık Tuşu Tam Desteği (MediaSession)
     */
    private fun ensureMediaSession(title: String) {
        val ctx = appContext ?: return
        try {
            if (mediaSession == null) {
                mediaSession = MediaSession(ctx, "AccessGrabMediaSession").apply {
                    setCallback(object : MediaSession.Callback() {
                        override fun onPlay() {
                            if (!_state.value.isPlaying) togglePlayPause()
                        }

                        override fun onPause() {
                            if (_state.value.isPlaying) togglePlayPause()
                        }

                        override fun onSkipToNext() {
                            playNext()
                        }

                        override fun onSkipToPrevious() {
                            playPrevious()
                        }

                        override fun onFastForward() {
                            seekBy(10_000)
                        }

                        override fun onRewind() {
                            seekBy(-10_000)
                        }

                        override fun onStop() {
                            stop()
                        }
                    })
                }
            }
            mediaSession?.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "AccessGrab")
                    .build()
            )
            mediaSession?.isActive = true
            updateMediaSessionState()
        } catch (_: Exception) {}
    }

    private fun updateMediaSessionState() {
        val session = mediaSession ?: return
        val s = _state.value
        try {
            val pbState = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_FAST_FORWARD or
                        PlaybackState.ACTION_REWIND or
                        PlaybackState.ACTION_STOP
                )
                .setState(
                    if (s.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    s.currentPositionMs.toLong(),
                    s.playbackSpeed
                )
                .build()
            session.setPlaybackState(pbState)
        } catch (_: Exception) {}
    }

    private fun releaseMediaSession() {
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {}
        mediaSession = null
    }

    private fun requestAudioFocus() {
        val ctx = appContext ?: return
        try {
            if (audioManager == null) {
                audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            }
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (_: Exception) {}
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusListener)
            }
        } catch (_: Exception) {}
    }

    private fun saveCurrentPositionIfApplicable() {
        if (_state.value.isRemotePreview) return
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
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null
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
                            updateMediaSessionState()
                            updateNotification()
                        }
                    }
                } catch (_: Exception) {}
                delay(400)
            }
        }
    }
}
