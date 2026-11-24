package com.baverelci.accessgrab.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import java.io.File
import java.io.FileInputStream

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

    fun setAsRingtone(
        context: Context,
        file: File,
        prefs: PreferencesManager
    ): Result<String> {
        return try {
            if (!canWriteSettings(context)) {
                requestWriteSettingsPermission(context)
                return Result.failure(
                    Exception("Zil sesini değiştirmek için lütfen açılan ekranda 'Sistem ayarlarını değiştirmeye izin ver' seçeneğini açıp geri gelin.")
                )
            }

            // Daha önce özel bir zil sesi atanmadıysa mevcut sistem zil sesini yedekle
            if (prefs.activeRingtonePath.value == null) {
                try {
                    val currentDefault = RingtoneManager.getActualDefaultRingtoneUri(
                        context,
                        RingtoneManager.TYPE_RINGTONE
                    )
                    if (currentDefault != null) {
                        prefs.setPreviousRingtoneUri(currentDefault.toString())
                    }
                } catch (_: Exception) {}
            }

            val cleanTitle = MediaFileManager.cleanDisplayName(file.nameWithoutExtension)
            val isMp3 = file.extension.lowercase() == "mp3"
            val mimeType = if (isMp3) "audio/mpeg" else "audio/mp4"
            val resolver = context.contentResolver

            val ringtoneUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "AccessGrab_Ringtone_${file.name}")
                    put(MediaStore.MediaColumns.TITLE, cleanTitle)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.IS_RINGTONE, true)
                    put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                    put(MediaStore.Audio.Media.IS_ALARM, false)
                    put(MediaStore.Audio.Media.IS_MUSIC, false)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
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
                    put(MediaStore.Audio.Media.IS_RINGTONE, true)
                }
                @Suppress("DEPRECATION")
                val contentUri = MediaStore.Audio.Media.getContentUriForPath(file.absolutePath)
                    ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                resolver.delete(contentUri, "${MediaStore.MediaColumns.DATA}=?", arrayOf(file.absolutePath))
                resolver.insert(contentUri, values)
            }

            if (ringtoneUri == null) {
                return Result.failure(Exception("Zil sesi medya deposuna kaydedilemedi."))
            }

            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                ringtoneUri
            )
            prefs.setActiveRingtonePath(file.absolutePath)
            AppLogger.log("RINGTONE_SET", "Zil sesi ayarlandı: $cleanTitle")
            Result.success("$cleanTitle telefon zil sesi olarak ayarlandı.")
        } catch (e: Exception) {
            AppLogger.log("RINGTONE_ERR", "Zil sesi ayarlanamadı", e)
            Result.failure(Exception("Zil sesi ayarlanamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}"))
        }
    }

    fun removeCustomRingtone(
        context: Context,
        prefs: PreferencesManager
    ): Result<String> {
        return try {
            if (!canWriteSettings(context)) {
                requestWriteSettingsPermission(context)
                return Result.failure(
                    Exception("Zil sesini sıfırlamak için lütfen açılan ekranda 'Sistem ayarlarını değiştirmeye izin ver' seçeneğini açıp geri gelin.")
                )
            }

            val prevUriStr = prefs.getPreviousRingtoneUri()
            val restoreUri = if (!prevUriStr.isNullOrBlank()) {
                Uri.parse(prevUriStr)
            } else {
                Settings.System.DEFAULT_RINGTONE_URI
            }

            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                restoreUri
            )
            prefs.setActiveRingtonePath(null)
            AppLogger.log("RINGTONE_REMOVE", "Özel zil sesi kaldırıldı ve önceki zil sesine dönüldü")
            Result.success("Zil sesi kaldırıldı ve önceki varsayılan zil sesine dönüldü.")
        } catch (e: Exception) {
            AppLogger.log("RINGTONE_ERR", "Zil sesi kaldırılamadı", e)
            Result.failure(Exception("Zil sesi kaldırılamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}"))
        }
    }
}
