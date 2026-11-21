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

    fun getLastNotifiedVersionCode(): Int {
        return prefs.getInt(KEY_LAST_NOTIFIED_VERSION, 0)
    }

    fun setLastNotifiedVersionCode(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, versionCode).apply()
    }

    companion object {
        private const val KEY_HAPTIC = "pref_haptic"
        private const val KEY_CLIPBOARD = "pref_clipboard"
        private const val KEY_DEFAULT_FORMAT = "pref_default_format"
        private const val KEY_AUTO_UPDATE = "pref_auto_update"
        private const val KEY_LAST_NOTIFIED_VERSION = "pref_last_notified_version"
    }
}
