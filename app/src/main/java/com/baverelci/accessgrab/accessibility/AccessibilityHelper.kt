package com.baverelci.accessgrab.accessibility

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
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

    /**
     * İndirme tamamlandığında uzaktan da duyulabilen hoş ve kısa bir onay sesi (bip/melodi) çalar.
     */
    fun playCompletionSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 260)
        } catch (_: Exception) {}
    }

    /**
     * Yeni bir güncelleme çıktığında hoş bir melodi/tonlama ile sesli uyarı verir.
     */
    fun playUpdateMelody(context: Context) {
        try {
            val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notifUri)
            if (ringtone != null) {
                ringtone.play()
                return
            }
        } catch (_: Exception) {}
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 320)
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
