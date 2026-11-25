package com.baverelci.accessgrab.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer

object RingtoneHelper {

    fun canWriteSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    fun requestWriteSettingsPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun setAsRingtone(context: Context, file: File, prefs: PreferencesManager): Result<String> {
        return setSystemTone(context, file, RingtoneManager.TYPE_RINGTONE, prefs)
    }

    fun setAsAlarm(context: Context, file: File, prefs: PreferencesManager): Result<String> {
        return setSystemTone(context, file, RingtoneManager.TYPE_ALARM, prefs)
    }

    fun setAsNotificationSound(context: Context, file: File, prefs: PreferencesManager): Result<String> {
        return setSystemTone(context, file, RingtoneManager.TYPE_NOTIFICATION, prefs)
    }

    private fun setSystemTone(
        context: Context,
        file: File,
        toneType: Int,
        prefs: PreferencesManager
    ): Result<String> {
        val label = when (toneType) {
            RingtoneManager.TYPE_ALARM -> "alarm sesi"
            RingtoneManager.TYPE_NOTIFICATION -> "bildirim sesi"
            else -> "telefon zil sesi"
        }

        return try {
            if (!canWriteSettings(context)) {
                requestWriteSettingsPermission(context)
                return Result.failure(
                    Exception("${label.replaceFirstChar { it.uppercase() }} ayarlamak için lütfen açılan ekranda 'Sistem ayarlarını değiştirmeye izin ver' seçeneğini açıp geri gelin.")
                )
            }

            // Mevcut sistem sesini yedekle
            when (toneType) {
                RingtoneManager.TYPE_RINGTONE -> {
                    if (prefs.activeRingtonePath.value == null) {
                        try {
                            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)?.let {
                                prefs.setPreviousRingtoneUri(it.toString())
                            }
                        } catch (_: Exception) {}
                    }
                }
                RingtoneManager.TYPE_ALARM -> {
                    if (prefs.activeAlarmPath.value == null) {
                        try {
                            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)?.let {
                                prefs.setPreviousAlarmUri(it.toString())
                            }
                        } catch (_: Exception) {}
                    }
                }
                RingtoneManager.TYPE_NOTIFICATION -> {
                    if (prefs.activeNotificationSoundPath.value == null) {
                        try {
                            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)?.let {
                                prefs.setPreviousNotificationSoundUri(it.toString())
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            val cleanTitle = MediaFileManager.cleanDisplayName(file.nameWithoutExtension)
            val ext = file.extension.lowercase()
            val mimeType = if (ext == "mp3") "audio/mpeg" else "audio/mp4"
            val resolver = context.contentResolver

            val isRing = toneType == RingtoneManager.TYPE_RINGTONE
            val isAlarm = toneType == RingtoneManager.TYPE_ALARM
            val isNotif = toneType == RingtoneManager.TYPE_NOTIFICATION
            val targetFolder = when (toneType) {
                RingtoneManager.TYPE_ALARM -> Environment.DIRECTORY_ALARMS
                RingtoneManager.TYPE_NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
                else -> Environment.DIRECTORY_RINGTONES
            }

            val toneUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "AccessGrab_${toneType}_${System.currentTimeMillis()}_${file.name}")
                    put(MediaStore.MediaColumns.TITLE, cleanTitle)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.IS_RINGTONE, isRing)
                    put(MediaStore.Audio.Media.IS_ALARM, isAlarm)
                    put(MediaStore.Audio.Media.IS_NOTIFICATION, isNotif)
                    put(MediaStore.Audio.Media.IS_MUSIC, false)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetFolder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val inserted = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                if (inserted != null) {
                    resolver.openOutputStream(inserted)?.use { outStream ->
                        FileInputStream(file).use { inStream ->
                            inStream.copyTo(outStream, 32768)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(inserted, values, null, null)
                }
                inserted
            } else {
                @Suppress("DEPRECATION")
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DATA, file.absolutePath)
                    put(MediaStore.MediaColumns.TITLE, cleanTitle)
                    put(MediaStore.MediaColumns.SIZE, file.length())
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.IS_RINGTONE, isRing)
                    put(MediaStore.Audio.Media.IS_ALARM, isAlarm)
                    put(MediaStore.Audio.Media.IS_NOTIFICATION, isNotif)
                }
                @Suppress("DEPRECATION")
                val contentUri = MediaStore.Audio.Media.getContentUriForPath(file.absolutePath)
                    ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                resolver.delete(contentUri, "${MediaStore.MediaColumns.DATA}=?", arrayOf(file.absolutePath))
                resolver.insert(contentUri, values)
            }

            if (toneUri == null) {
                return Result.failure(Exception("Ses dosyası sistem medya deposuna kaydedilemedi."))
            }

            RingtoneManager.setActualDefaultRingtoneUri(context, toneType, toneUri)
            when (toneType) {
                RingtoneManager.TYPE_RINGTONE -> prefs.setActiveRingtonePath(file.absolutePath)
                RingtoneManager.TYPE_ALARM -> prefs.setActiveAlarmPath(file.absolutePath)
                RingtoneManager.TYPE_NOTIFICATION -> prefs.setActiveNotificationSoundPath(file.absolutePath)
            }

            AppLogger.log("TONE_SET", "$cleanTitle -> $label olarak ayarlandı")
            Result.success("$cleanTitle $label olarak ayarlandı.")
        } catch (e: Exception) {
            AppLogger.log("TONE_ERR", "$label ayarlanamadı", e)
            Result.failure(Exception("Ayarlanamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}"))
        }
    }

    fun removeCustomRingtone(context: Context, prefs: PreferencesManager): Result<String> {
        return removeSystemTone(context, RingtoneManager.TYPE_RINGTONE, prefs)
    }

    fun removeCustomAlarm(context: Context, prefs: PreferencesManager): Result<String> {
        return removeSystemTone(context, RingtoneManager.TYPE_ALARM, prefs)
    }

    fun removeCustomNotificationSound(context: Context, prefs: PreferencesManager): Result<String> {
        return removeSystemTone(context, RingtoneManager.TYPE_NOTIFICATION, prefs)
    }

    private fun removeSystemTone(
        context: Context,
        toneType: Int,
        prefs: PreferencesManager
    ): Result<String> {
        val label = when (toneType) {
            RingtoneManager.TYPE_ALARM -> "alarm sesi"
            RingtoneManager.TYPE_NOTIFICATION -> "bildirim sesi"
            else -> "zil sesi"
        }

        return try {
            if (!canWriteSettings(context)) {
                requestWriteSettingsPermission(context)
                return Result.failure(
                    Exception("Sesi sıfırlamak için lütfen açılan ekranda 'Sistem ayarlarını değiştirmeye izin ver' seçeneğini açıp geri gelin.")
                )
            }

            val prevUriStr = when (toneType) {
                RingtoneManager.TYPE_ALARM -> prefs.getPreviousAlarmUri()
                RingtoneManager.TYPE_NOTIFICATION -> prefs.getPreviousNotificationSoundUri()
                else -> prefs.getPreviousRingtoneUri()
            }
            val defaultFallback = when (toneType) {
                RingtoneManager.TYPE_ALARM -> Settings.System.DEFAULT_ALARM_ALERT_URI
                RingtoneManager.TYPE_NOTIFICATION -> Settings.System.DEFAULT_NOTIFICATION_URI
                else -> Settings.System.DEFAULT_RINGTONE_URI
            }
            val restoreUri = if (!prevUriStr.isNullOrBlank()) Uri.parse(prevUriStr) else defaultFallback

            RingtoneManager.setActualDefaultRingtoneUri(context, toneType, restoreUri)
            when (toneType) {
                RingtoneManager.TYPE_RINGTONE -> prefs.setActiveRingtonePath(null)
                RingtoneManager.TYPE_ALARM -> prefs.setActiveAlarmPath(null)
                RingtoneManager.TYPE_NOTIFICATION -> prefs.setActiveNotificationSoundPath(null)
            }

            Result.success("Özel $label kaldırıldı ve önceki varsayılan sese dönüldü.")
        } catch (e: Exception) {
            Result.failure(Exception("Kaldırılamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}"))
        }
    }

    /**
     * #25 — Şarkının veya videonun oynatıcıdaki mevcut saniyesinden (startMs) itibaren
     * 30 saniyelik nakarat/bölümünü keser, Kütüphane'ye kaydeder ve doğrudan Zil Sesi yapar.
     */
    fun trim30SecAndSetRingtone(
        context: Context,
        sourceFile: File,
        startMs: Int,
        totalDurationMs: Int,
        prefs: PreferencesManager
    ): Result<String> {
        if (!canWriteSettings(context)) {
            requestWriteSettingsPermission(context)
            return Result.failure(
                Exception("Zil sesi kesip ayarlamak için lütfen açılan ekranda 'Sistem ayarlarını değiştirmeye izin ver' seçeneğini açıp geri gelin.")
            )
        }

        val cleanName = MediaFileManager.cleanDisplayName(sourceFile.nameWithoutExtension)
            .replace(" (30sn Zil Sesi)", "")
            .take(45)
        val startSec = (startMs / 1000).coerceAtLeast(0)
        val clipDurationMs = 30_000
        val ext = sourceFile.extension.lowercase()

        val targetDir = MediaFileManager.getAccessGrabDirectory().apply { mkdirs() }
        val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "AccessGrab").apply { mkdirs() }

        val outFileName = "${cleanName}_${startSec}sn_ZilSesi.${if (ext == "mp3") "mp3" else "mp4"}"
        val outFile = if (targetDir.canWrite()) File(targetDir, outFileName) else File(fallbackDir, outFileName)

        return try {
            val clippedOk = if (ext == "mp3") {
                sliceMp3FileByTime(sourceFile, outFile, startMs, clipDurationMs, totalDurationMs)
            } else {
                muxAudioSegment(sourceFile, outFile, startMs, clipDurationMs) ||
                    sliceMp3FileByTime(sourceFile, outFile, startMs, clipDurationMs, totalDurationMs)
            }

            val finalToneFile = if (clippedOk && outFile.exists() && outFile.length() > 1024L) outFile else sourceFile
            val setRes = setAsRingtone(context, finalToneFile, prefs)
            if (setRes.isSuccess) {
                val mins = startSec / 60
                val secs = startSec % 60
                val timeFormatted = String.format("%02d:%02d", mins, secs)
                Result.success("$cleanName ($timeFormatted saniyesinden itibaren 30 saniye) kesilerek telefon zil sesi yapıldı.")
            } else {
                setRes
            }
        } catch (e: Exception) {
            Result.failure(Exception("Bölüm kesilemedi: ${e.localizedMessage}"))
        }
    }

    private fun sliceMp3FileByTime(
        source: File,
        dest: File,
        startMs: Int,
        clipDurationMs: Int,
        totalDurationMs: Int
    ): Boolean {
        return try {
            val totalLen = source.length()
            if (totalLen <= 0L || totalDurationMs <= 0) return false

            val startRatio = (startMs.toDouble() / totalDurationMs.toDouble()).coerceIn(0.0, 0.95)
            val endRatio = ((startMs + clipDurationMs).toDouble() / totalDurationMs.toDouble()).coerceIn(startRatio + 0.02, 1.0)

            val startByte = (totalLen * startRatio).toLong()
            val endByte = (totalLen * endRatio).toLong()
            val bytesToCopy = (endByte - startByte).coerceAtLeast(4096L)

            RandomAccessFile(source, "r").use { raf ->
                raf.seek(startByte)
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(16384)
                    var remaining = bytesToCopy
                    while (remaining > 0) {
                        val toRead = minOf(buf.size.toLong(), remaining).toInt()
                        val read = raf.read(buf, 0, toRead)
                        if (read <= 0) break
                        out.write(buf, 0, read)
                        remaining -= read
                    }
                    out.flush()
                }
            }
            dest.exists() && dest.length() > 1024L
        } catch (_: Exception) {
            false
        }
    }

    private fun muxAudioSegment(
        source: File,
        dest: File,
        startMs: Int,
        clipDurationMs: Int
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(source.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) return false
            extractor.selectTrack(audioTrackIndex)

            val startUs = startMs.toLong() * 1000L
            val endUs = (startMs + clipDurationMs).toLong() * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            if (dest.exists()) dest.delete()
            muxer = MediaMuxer(dest.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break
                if (sampleTimeUs >= startUs) {
                    bufferInfo.presentationTimeUs = sampleTimeUs - startUs
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxTrack, buffer, bufferInfo)
                }
                extractor.advance()
            }

            muxer.stop()
            dest.exists() && dest.length() > 1024L
        } catch (_: Exception) {
            false
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
