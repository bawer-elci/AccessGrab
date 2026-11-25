package com.baverelci.accessgrab.downloader

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import com.baverelci.accessgrab.data.AppLogger
import com.baverelci.accessgrab.network.AccessGrabApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

object DownloadHelper {

    @Volatile
    private var activeCall: Call? = null

    /**
     * Devam eden aktif HTTP indirme bağlantısını anında keser.
     */
    fun cancelCurrentCall() {
        try {
            activeCall?.cancel()
        } catch (_: Exception) {}
        activeCall = null
    }

    /**
     * Redmi (MIUI / HyperOS), Xiaomi, Oppo, Vivo, Huawei ve Samsung dahil tüm cihazlarda
     * sistem DownloadManager kısıtlamalarına veya 20 sn zaman aşımına (HTTP 499) takılmadan
     * doğrudan uygulama içi yüksek hızlı akış (streaming) sağlayan OkHttp istemcisi.
     */
    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun sanitizeFileName(title: String, format: String): String {
        val ext = if (format.lowercase() == "mp3") "mp3" else "mp4"
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ\\s_-]"), "")
            .trim()
            .take(60)
            .ifEmpty { "AccessGrab_Media" }
        return "$sanitizedTitle.$ext"
    }

    /**
     * Android 7.0 (API 24) - Android 16 (API 35+) arası tüm sürümlerde %100 garantili
     * doğrudan medya indirme motoru.
     */
    suspend fun downloadMediaDirect(
        context: Context,
        mediaUrl: String,
        format: String,
        title: String,
        quality: String? = null,
        onProgress: (progress: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadUrl = AccessGrabApi.buildDownloadUrl(mediaUrl, format, quality)
        val ext = if (format.lowercase() == "mp3") "mp3" else "mp4"
        val mimeType = if (ext == "mp3") "audio/mpeg" else "video/mp4"
        val fileName = sanitizeFileName(title, format)

        val appBaseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val appAccessGrabDir = File(appBaseDir, "AccessGrab").apply { mkdirs() }
        val tempFile = File(appAccessGrabDir, "$fileName.part")
        val finalAppFile = File(appAccessGrabDir, fileName)

        // #39 — Ekran kapansa bile Wi-Fi ve işlemcinin uyku moduna geçip indirmeyi yavaşlatmasını önleyen kilitler
        val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AccessGrab:DownloadWakeLock")
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "AccessGrab:DownloadWifiLock")

        try {
            try { wakeLock?.acquire(15 * 60 * 1000L) } catch (_: Exception) {}
            try { wifiLock?.acquire() } catch (_: Exception) {}

            // #32 — İnternet anlık koparsa kaldığı megabayttan (HTTP Range ile) devam etme döngüsü
            var attempt = 0
            val maxAttempts = 3
            var lastException: Exception? = null

            while (attempt < maxAttempts) {
                attempt++
                val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

                try {
                    val reqBuilder = Request.Builder()
                        .url(downloadUrl)
                        .header("X-AccessGrab-Key", AccessGrabApi.API_KEY)

                    if (existingBytes > 0L) {
                        reqBuilder.header("Range", "bytes=$existingBytes-")
                        AppLogger.log("DOWNLOAD_RESUME", "Kaldığı yerden devam ediliyor: $existingBytes bayt (Deneme $attempt)")
                    }

                    val call = downloadClient.newCall(reqBuilder.build())
                    activeCall = call

                    call.execute().use { response ->
                        coroutineContext.ensureActive()
                        AppLogger.log("DOWNLOAD_HTTP", "HTTP ${response.code} yanıtı alındı ($fileName)")
                        if (!response.isSuccessful && response.code != 206) {
                            val code = response.code
                            if (code == 416 && existingBytes > 0L) {
                                // Range Not Satisfiable -> sıfırdan başlat
                                tempFile.delete()
                                return@use
                            }
                            val errMsg = when (code) {
                                429 -> "Çok fazla indirme isteği gönderildi. Lütfen 1 dakika bekleyip tekrar deneyin."
                                504 -> "Medya sunucuda hazırlanırken zaman aşımına uğradı. Lütfen tekrar deneyin."
                                else -> "Sunucu hatası (HTTP $code). Lütfen tekrar deneyin."
                            }
                            return@withContext Result.failure(Exception(errMsg))
                        }

                        val body = response.body
                            ?: return@withContext Result.failure(Exception("Sunucudan boş medya yanıtı alındı."))

                        val isResuming = response.code == 206 && existingBytes > 0L
                        val bodyLen = body.contentLength()
                        val totalBytes = if (isResuming && bodyLen > 0L) existingBytes + bodyLen else bodyLen
                        var downloadedBytes = if (isResuming) existingBytes else 0L
                        var lastReportTime = 0L

                        body.byteStream().use { input ->
                            FileOutputStream(tempFile, isResuming).use { output ->
                                val buffer = ByteArray(131072)
                                var bytesRead: Int

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    coroutineContext.ensureActive()
                                    output.write(buffer, 0, bytesRead)
                                    downloadedBytes += bytesRead

                                    val now = System.currentTimeMillis()
                                    if (now - lastReportTime >= 120 || (totalBytes > 0L && downloadedBytes == totalBytes)) {
                                        lastReportTime = now
                                        val progress = if (totalBytes > 0L) {
                                            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                        } else {
                                            0
                                        }
                                        onProgress(progress, downloadedBytes, totalBytes)
                                    }
                                }
                                output.flush()
                            }
                        }

                        if (downloadedBytes <= 0L) {
                            tempFile.delete()
                            return@withContext Result.failure(Exception("İndirilen medya dosyası boş."))
                        }

                        if (finalAppFile.exists()) finalAppFile.delete()
                        if (!tempFile.renameTo(finalAppFile)) {
                            tempFile.copyTo(finalAppFile, overwrite = true)
                            tempFile.delete()
                        }

                        val exportedFile = exportToPublicDownloads(context, finalAppFile, fileName, mimeType)
                        val resultFile = exportedFile ?: finalAppFile

                        onProgress(100, downloadedBytes, if (totalBytes > 0L) totalBytes else downloadedBytes)
                        activeCall = null
                        return@withContext Result.success(resultFile)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastException = e
                    coroutineContext.ensureActive()
                    if (attempt < maxAttempts) {
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }

            activeCall = null
            Result.failure(lastException ?: Exception("İndirme bağlantısı kesildi."))
        } catch (e: CancellationException) {
            activeCall = null
            try { tempFile.delete() } catch (_: Exception) {}
            throw e
        } catch (e: Exception) {
            activeCall = null
            Result.failure(e)
        } finally {
            try { if (wakeLock?.isHeld == true) wakeLock.release() } catch (_: Exception) {}
            try { if (wifiLock?.isHeld == true) wifiLock.release() } catch (_: Exception) {}
        }
    }

    /**
     * İndirilen dosyayı telefonun genel İndirilenler/AccessGrab klasörüne de kopyalar ve
     * MediaScanner ile Galeri / Müzik uygulamalarına bildirir.
     */
    private fun exportToPublicDownloads(
        context: Context,
        sourceFile: File,
        fileName: String,
        mimeType: String
    ): File? {
        // A) Doğrudan genel Download/AccessGrab dizinine yazmayı dene (Android 7-16 FUSE)
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val publicAccessGrabDir = File(publicDownloads, "AccessGrab")
            if (!publicAccessGrabDir.exists()) {
                publicAccessGrabDir.mkdirs()
            }
            val targetFile = File(publicAccessGrabDir, fileName)
            sourceFile.copyTo(targetFile, overwrite = true)
            if (targetFile.exists() && targetFile.length() == sourceFile.length()) {
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                } catch (_: Exception) {}
                return targetFile
            }
        } catch (_: Exception) {}

        // B) Android 10+ (API 29+) Scoped Storage MediaStore.Downloads yedek aktarımı
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AccessGrab")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outStream ->
                        FileInputStream(sourceFile).use { inStream ->
                            inStream.copyTo(outStream, 32768)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            } catch (_: Exception) {}
        }

        return null
    }
}
