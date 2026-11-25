package com.baverelci.accessgrab.accessibility

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import com.baverelci.accessgrab.R

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
     * İndirme tamamlandığında uzaktan da duyulabilen hoş ve kısa bir onay sesi çalar.
     */
    fun playCompletionSound() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 260)
        } catch (_: Exception) {}
    }

    /**
     * Duyuru ve yeni güncelleme bildirimlerinde özel bestelenmiş kristal melodi (accessgrab_chime.wav) çalar.
     */
    fun playCustomNotificationChime(context: Context) {
        try {
            val afd = context.applicationContext.resources.openRawResourceFd(R.raw.accessgrab_chime) ?: return
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { player ->
                try { player.release() } catch (_: Exception) {}
            }
            mp.setOnErrorListener { player, _, _ ->
                try { player.release() } catch (_: Exception) {}
                true
            }
            mp.prepare()
            mp.start()
            return
        } catch (_: Exception) {}

        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 320)
        } catch (_: Exception) {}
    }

    /**
     * Yeni bir güncelleme çıktığında hoş bir melodi/tonlama ile sesli uyarı verir.
     */
    fun playUpdateMelody(context: Context) {
        playCustomNotificationChime(context)
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
