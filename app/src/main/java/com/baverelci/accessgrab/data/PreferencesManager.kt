package com.baverelci.accessgrab.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("accessgrab_prefs", Context.MODE_PRIVATE)

    private val _isHapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, true))
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    private val _isClipboardEnabled = MutableStateFlow(prefs.getBoolean(KEY_CLIPBOARD, true))
    val isClipboardEnabled: StateFlow<Boolean> = _isClipboardEnabled.asStateFlow()

    private val _defaultFormat = MutableStateFlow(prefs.getString(KEY_DEFAULT_FORMAT, "ask") ?: "ask")
    val defaultFormat: StateFlow<String> = _defaultFormat.asStateFlow()

    // Kullanıcı isteği doğrultusunda varsayılan olarak KAPALI (false)
    private val _isAutoUpdateEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE, false))
    val isAutoUpdateEnabled: StateFlow<Boolean> = _isAutoUpdateEnabled.asStateFlow()

    // Telefonu sallayarak oynatıcıyı duraklat/başlat (Varsayılan: Açık)
    private val _isShakeToPlayPauseEnabled = MutableStateFlow(prefs.getBoolean(KEY_SHAKE_PLAY_PAUSE, true))
    val isShakeToPlayPauseEnabled: StateFlow<Boolean> = _isShakeToPlayPauseEnabled.asStateFlow()

    // İndirme tamamlandığında özel onay sesi (Varsayılan: Açık)
    private val _isCompletionSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_COMPLETION_SOUND, true))
    val isCompletionSoundEnabled: StateFlow<Boolean> = _isCompletionSoundEnabled.asStateFlow()

    // Aktif özel zil sesi dosya yolu (Varsa)
    private val _activeRingtonePath = MutableStateFlow(prefs.getString(KEY_ACTIVE_RINGTONE_PATH, null))
    val activeRingtonePath: StateFlow<String?> = _activeRingtonePath.asStateFlow()

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _isHapticEnabled.value = enabled
    }

    fun setClipboardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLIPBOARD, enabled).apply()
        _isClipboardEnabled.value = enabled
    }

    fun setDefaultFormat(format: String) {
        prefs.edit().putString(KEY_DEFAULT_FORMAT, format).apply()
        _defaultFormat.value = format
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
        _isAutoUpdateEnabled.value = enabled
    }

    fun setShakeToPlayPauseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHAKE_PLAY_PAUSE, enabled).apply()
        _isShakeToPlayPauseEnabled.value = enabled
    }

    fun setCompletionSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COMPLETION_SOUND, enabled).apply()
        _isCompletionSoundEnabled.value = enabled
    }

    fun getLastNotifiedVersionCode(): Int {
        return prefs.getInt(KEY_LAST_NOTIFIED_VERSION, 0)
    }

    fun setLastNotifiedVersionCode(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, versionCode).apply()
    }

    /**
     * "Kaldığım Yerden Devam Et" için dosya adına göre son oynatma konumunu (ms) kaydeder.
     */
    fun savePlaybackPosition(fileKey: String, positionMs: Int) {
        prefs.edit().putInt("pos_$fileKey", positionMs).apply()
    }

    fun getSavedPlaybackPosition(fileKey: String): Int {
        return prefs.getInt("pos_$fileKey", 0)
    }

    fun clearSavedPlaybackPosition(fileKey: String) {
        prefs.edit().remove("pos_$fileKey").apply()
    }

    /**
     * Zil sesi yap/kaldır durumu ve önceki varsayılan zil sesi yedeği
     */
    fun setActiveRingtonePath(path: String?) {
        if (path == null) {
            prefs.edit().remove(KEY_ACTIVE_RINGTONE_PATH).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_RINGTONE_PATH, path).apply()
        }
        _activeRingtonePath.value = path
    }

    fun getPreviousRingtoneUri(): String? {
        return prefs.getString(KEY_PREV_RINGTONE_URI, null)
    }

    fun setPreviousRingtoneUri(uriStr: String?) {
        if (uriStr == null) {
            prefs.edit().remove(KEY_PREV_RINGTONE_URI).apply()
        } else {
            prefs.edit().putString(KEY_PREV_RINGTONE_URI, uriStr).apply()
        }
    }

    companion object {
        private const val KEY_HAPTIC = "pref_haptic"
        private const val KEY_CLIPBOARD = "pref_clipboard"
        private const val KEY_DEFAULT_FORMAT = "pref_default_format"
        private const val KEY_AUTO_UPDATE = "pref_auto_update"
        private const val KEY_LAST_NOTIFIED_VERSION = "pref_last_notified_version"
        private const val KEY_SHAKE_PLAY_PAUSE = "pref_shake_play_pause"
        private const val KEY_COMPLETION_SOUND = "pref_completion_sound"
        private const val KEY_ACTIVE_RINGTONE_PATH = "pref_active_ringtone_path"
        private const val KEY_PREV_RINGTONE_URI = "pref_prev_ringtone_uri"
    }
}
