package com.baverelci.accessgrab.data

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

    fun getAccessGrabDirectory(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, "AccessGrab")
    }

    fun getDownloadedFiles(): List<DownloadedMediaFile> {
        val dir = getAccessGrabDirectory()
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        val files = dir.listFiles { f ->
            f.isFile && (f.name.endsWith(".mp4", ignoreCase = true) || f.name.endsWith(".mp3", ignoreCase = true))
        } ?: return emptyList()

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR"))

        return files.map { file ->
            val ext = file.extension.lowercase()
            val isAudio = ext == "mp3"
            DownloadedMediaFile(
                file = file,
                name = file.nameWithoutExtension,
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
            val mimeType = if (ext == "mp3") "audio/*" else "video/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
            val mimeType = if (ext == "mp3") "audio/*" else "video/*"

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

    fun deleteMediaFile(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
