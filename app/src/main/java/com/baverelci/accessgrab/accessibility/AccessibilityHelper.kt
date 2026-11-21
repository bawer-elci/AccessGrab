package com.baverelci.accessgrab.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View

object AccessibilityHelper {

    /**
     * TalkBack veya ekran okuyucuya doğrudan yüksek öncelikli sesli duyuru gönderir.
     */
    fun announce(view: View, message: String) {
        view.announceForAccessibility(message)
    }

    /**
     * İşlem başarılı olduğunda hafif ve tatmin edici bir onay titreşimi üretir.
     */
    fun vibrateSuccess(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    /**
     * Hata durumunda iki kısa uyarı titreşimi üretir.
     */
    fun vibrateError(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 80, 120)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 80, 80, 120), -1)
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
