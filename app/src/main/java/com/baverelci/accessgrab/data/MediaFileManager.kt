package com.baverelci.accessgrab.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.ArrayList
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

    /**
     * #76 — WhatsApp Durumu İçin Videoyu 30 Saniyelik Parçalara Bölerek Paylaşma
     */
    fun splitVideoInto30SecAndShare(context: Context, sourceFile: File): Result<String> {
        return try {
            if (!sourceFile.exists()) {
                return Result.failure(Exception("Video dosyası bulunamadı."))
            }

            var totalDurationMs = 0L
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(sourceFile.absolutePath)
                totalDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (_: Exception) {
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }

            if (totalDurationMs in 1..32_000L) {
                shareMediaFile(context, sourceFile)
                return Result.success("Video 30 saniyeden kısa olduğu için doğrudan paylaşım ekranı açıldı.")
            }

            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "AccessGrab") }
                ?: File(context.filesDir, "AccessGrab")
            if (!baseDir.exists()) baseDir.mkdirs()

            val baseTitle = cleanDisplayName(sourceFile.nameWithoutExtension).take(35).trim()
            val segmentDurationUs = 30_000_000L // 30 saniye
            val totalDurationUs = if (totalDurationMs > 0) totalDurationMs * 1000L else 180_000_000L
            val maxParts = ((totalDurationUs + segmentDurationUs - 1) / segmentDurationUs).toInt().coerceIn(1, 10)

            val createdFiles = mutableListOf<File>()

            for (partIdx in 0 until maxParts) {
                val startUs = partIdx * segmentDurationUs
                val endUs = startUs + segmentDurationUs
                if (startUs >= totalDurationUs) break

                val outFile = File(baseDir, "${baseTitle}_Durum_${partIdx + 1}.mp4")
                if (outFile.exists()) outFile.delete()

                val extractor = MediaExtractor()
                var muxer: MediaMuxer? = null
                var wroteSamples = false

                try {
                    extractor.setDataSource(sourceFile.absolutePath)
                    muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                    val trackMap = mutableMapOf<Int, Int>()
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                            extractor.selectTrack(i)
                            trackMap[i] = muxer.addTrack(format)
                        }
                    }

                    if (trackMap.isEmpty()) break

                    muxer.start()
                    extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                    val buffer = ByteBuffer.allocate(512 * 1024)
                    val bufferInfo = MediaCodec.BufferInfo()
                    var firstSampleTimeUs = -1L

                    while (true) {
                        val trackIndex = extractor.sampleTrackIndex
                        if (trackIndex < 0) break

                        val sampleTimeUs = extractor.sampleTime
                        if (sampleTimeUs > endUs) break

                        val muxerTrackIdx = trackMap[trackIndex]
                        if (muxerTrackIdx != null && sampleTimeUs >= startUs - 500_000L) {
                            bufferInfo.offset = 0
                            bufferInfo.size = extractor.readSampleData(buffer, 0)
                            if (bufferInfo.size < 0) break

                            if (firstSampleTimeUs < 0) firstSampleTimeUs = sampleTimeUs
                            bufferInfo.presentationTimeUs = (sampleTimeUs - firstSampleTimeUs).coerceAtLeast(0L)
                            bufferInfo.flags = extractor.sampleFlags
                            muxer.writeSampleData(muxerTrackIdx, buffer, bufferInfo)
                            wroteSamples = true
                        }
                        extractor.advance()
                    }
                } catch (_: Exception) {
                } finally {
                    try { muxer?.stop() } catch (_: Exception) {}
                    try { muxer?.release() } catch (_: Exception) {}
                    try { extractor.release() } catch (_: Exception) {}
                }

                if (wroteSamples && outFile.exists() && outFile.length() > 10_000L) {
                    createdFiles.add(outFile)
                } else {
                    outFile.delete()
                }
            }

            if (createdFiles.isEmpty()) {
                shareMediaFile(context, sourceFile)
                return Result.success("Paylaşım ekranı açıldı.")
            }

            val uris = ArrayList<Uri>()
            for (f in createdFiles) {
                uris.add(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        f
                    )
                )
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/mp4"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "30 Saniyelik Durum Parçalarını Paylaş (${createdFiles.size} Parça)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Result.success("Video ${createdFiles.size} adet 30 saniyelik WhatsApp Durum parçasına bölündü, kütüphaneye kaydedildi ve paylaşım ekranı açıldı.")
        } catch (e: Exception) {
            Result.failure(Exception("Video parçalara bölünemedi: ${e.localizedMessage}"))
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
