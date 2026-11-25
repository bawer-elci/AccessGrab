package com.baverelci.accessgrab.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DownloadedMediaFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String,
    val isAudio: Boolean,
    val extension: String
)

object MediaFileManager {

    private val TRASHED_PREFIX_REGEX = Regex("^\\.?trashed-(\\d+-)?", RegexOption.IGNORE_CASE)

    /**
     * Android MediaStore çöp kutusu ön eklerini (.trashed-1766589123- vb.) temizleyerek
     * ekran okuyucu ve görsel arayüz için saf medya başlığını döndürür.
     */
    fun cleanDisplayName(rawNameWithoutExt: String): String {
        return rawNameWithoutExt
            .replace(TRASHED_PREFIX_REGEX, "")
            .trim()
            .ifEmpty { "İndirilen Medya" }
    }

    fun getAccessGrabDirectory(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, "AccessGrab")
    }

    fun getDownloadedFiles(context: Context? = null): List<DownloadedMediaFile> {
        val collectedFiles = linkedMapOf<String, File>()

        fun addFileIfNotDuplicate(file: File) {
            if (!file.exists() || !file.isFile || file.length() <= 0L) return
            val lowerName = file.name.lowercase()
            if (!lowerName.endsWith(".mp4") && !lowerName.endsWith(".mp3")) return
            val cleanKey = "${cleanDisplayName(file.nameWithoutExtension).lowercase()}.${file.extension.lowercase()}"
            if (!collectedFiles.containsKey(cleanKey)) {
                collectedFiles[cleanKey] = file
            }
        }

        // 1. Genel Download/AccessGrab klasöründeki medya dosyalarını tara
        val publicDir = getAccessGrabDirectory()
        if (publicDir.exists() && publicDir.isDirectory) {
            publicDir.listFiles()?.forEach { addFileIfNotDuplicate(it) }
        }

        // 2. Uygulama özel medya dizinlerini tara (Android 7 - 16 %100 garantili dizin)
        if (context != null) {
            try {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { extBase ->
                    val appExtDir = File(extBase, "AccessGrab")
                    if (appExtDir.exists() && appExtDir.isDirectory) {
                        appExtDir.listFiles()?.forEach { addFileIfNotDuplicate(it) }
                    }
                }
            } catch (_: Exception) {}

            try {
                val appIntDir = File(context.filesDir, "AccessGrab")
                if (appIntDir.exists() && appIntDir.isDirectory) {
                    appIntDir.listFiles()?.forEach { addFileIfNotDuplicate(it) }
                }
            } catch (_: Exception) {}

            // 3. Sistem DownloadManager kayıtlarından eski indirmeleri de doğrula
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (dm != null) {
                    val query = DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
                    dm.query(query)?.use { cursor ->
                        val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        while (cursor.moveToNext()) {
                            if (localUriIdx >= 0) {
                                val localUriStr = cursor.getString(localUriIdx) ?: continue
                                val uri = Uri.parse(localUriStr)
                                if (uri.scheme == "file") {
                                    val path = uri.path ?: continue
                                    addFileIfNotDuplicate(File(path))
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (collectedFiles.isEmpty()) return emptyList()

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR"))

        return collectedFiles.values.map { file ->
            val ext = file.extension.lowercase()
            val isAudio = ext == "mp3"
            val cleanName = cleanDisplayName(file.nameWithoutExtension)
            DownloadedMediaFile(
                file = file,
                name = cleanName,
                sizeBytes = file.length(),
                formattedSize = formatBytes(file.length()),
                lastModified = file.lastModified(),
                formattedDate = dateFormat.format(Date(file.lastModified())),
                isAudio = isAudio,
                extension = ext.uppercase()
            )
        }.sortedByDescending { it.lastModified }
    }

    fun openMediaFile(context: Context, file: File): Result<Unit> {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val ext = file.extension.lowercase()
            val mimeType = if (ext == "mp3") "audio/mpeg" else "video/mp4"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val ext = file.extension.lowercase()
                val fallbackMime = if (ext == "mp3") "audio/*" else "video/*"
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, fallbackMime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                Result.success(Unit)
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    fun shareMediaFile(context: Context, file: File): Result<Unit> {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val ext = file.extension.lowercase()
            val mimeType = if (ext == "mp3") "audio/mpeg" else "video/mp4"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Medyayı Paylaş").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteMediaFile(file: File, context: Context? = null): Boolean {
        var deleted = false
        val targetCleanKey = "${cleanDisplayName(file.nameWithoutExtension).lowercase()}.${file.extension.lowercase()}"

        try {
            if (file.exists() && file.delete()) {
                deleted = true
            }
        } catch (_: Exception) {}

        // Hem genel Download/AccessGrab hem de uygulama içi dizindeki eşleşen kopyaları temizle
        val candidateDirs = mutableListOf(getAccessGrabDirectory())
        if (context != null) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                candidateDirs.add(File(it, "AccessGrab"))
            }
            candidateDirs.add(File(context.filesDir, "AccessGrab"))
        }

        for (dir in candidateDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { candidate ->
                        val key = "${cleanDisplayName(candidate.nameWithoutExtension).lowercase()}.${candidate.extension.lowercase()}"
                        if (candidate.name == file.name || key == targetCleanKey) {
                            if (candidate.delete()) {
                                deleted = true
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (context != null) {
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (dm != null) {
                    val query = DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
                    dm.query(query)?.use { cursor ->
                        val idIdx = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                        val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        while (cursor.moveToNext()) {
                            if (idIdx >= 0 && localUriIdx >= 0) {
                                val localUriStr = cursor.getString(localUriIdx) ?: continue
                                val path = Uri.parse(localUriStr).path ?: continue
                                val f = File(path)
                                val key = "${cleanDisplayName(f.nameWithoutExtension).lowercase()}.${f.extension.lowercase()}"
                                if (path == file.absolutePath || f.name == file.name || key == targetCleanKey) {
                                    val id = cursor.getLong(idIdx)
                                    if (dm.remove(id) > 0) {
                                        deleted = true
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return deleted || !file.exists()
    }

    /**
     * Kütüphanedeki tüm indirilen medya dosyalarını tek seferde kalıcı olarak temizler.
     */
    fun deleteAllMediaFiles(context: Context? = null): Int {
        val currentList = getDownloadedFiles(context)
        var deletedCount = 0
        for (item in currentList) {
            if (deleteMediaFile(item.file, context)) {
                deletedCount++
            }
        }
        return deletedCount
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
