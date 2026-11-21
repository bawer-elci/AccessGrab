package com.baverelci.accessgrab.downloader

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object ApkInstallHelper {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Uygulamanın doğrudan güncelleme kurma (Bilinmeyen kaynaklardan yükleme) izni olup olmadığını denetler.
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Kullanıcıyı 'Bu kaynaktan uygulama yüklemeye izin ver' sistem ayarlarına yönlendirir.
     */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val generalIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(generalIntent)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * İndirilen güncelleme dosyasını döndürür.
     */
    fun getDownloadedUpdateFile(context: Context): File? {
        val cacheFile = File(context.cacheDir, "AccessGrab_Update.apk")
        if (cacheFile.exists() && cacheFile.length() > 100_000L) return cacheFile

        val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (extDir != null) {
            val extFile = File(extDir, "AccessGrab_Update.apk")
            if (extFile.exists() && extFile.length() > 100_000L) return extFile
        }
        return null
    }

    /**
     * Eski güncelleme dosyasını temizler.
     */
    fun clearDownloadedUpdateFile(context: Context) {
        try {
            File(context.cacheDir, "AccessGrab_Update.apk").delete()
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                File(it, "AccessGrab_Update.apk").delete()
            }
        } catch (_: Exception) {}
    }

    /**
     * Güncelleme APK'sını sunucudan indirir.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(apkUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Sunucudan APK indirilemedi: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Boş yanıt alındı"))
            val contentLength = body.contentLength()

            // Güvenli dahili önbellek dizini (FileProvider ile %100 uyumlu)
            val updateFile = File(context.cacheDir, "AccessGrab_Update.apk")
            if (updateFile.exists()) updateFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(updateFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = ((totalRead * 100) / contentLength).toInt()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            updateFile.setReadable(true, false)
            Result.success(updateFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * İndirilen APK dosyasını doğrudan sistem paket yükleyicisi ile uygulamanın üzerine kurar.
     */
    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(Exception("Kurulum dosyası bulunamadı."))
            }

            apkFile.setReadable(true, false)

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            // Android standart temiz kurulum intent'i (CLEAR_TOP veya yetkisiz EXTRA olmadan)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Android 11+ Package Visibility için sorgulanan paket yükleyicilere okuma yetkisi tanımla
            val resInfoList = context.packageManager.queryIntentActivities(
                installIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Bilinen sistem paket yükleyicilerine (Pixel, MIUI, OneUI vb.) açıkça izin tanımla
            val knownInstallers = listOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.miui.packageinstaller",
                "com.samsung.android.packageinstaller"
            )
            for (pkg in knownInstallers) {
                try {
                    context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
