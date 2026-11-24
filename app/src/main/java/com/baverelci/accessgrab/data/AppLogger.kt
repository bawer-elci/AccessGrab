package com.baverelci.accessgrab.data

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val MAX_ENTRIES = 120
    private const val LOG_FILE_NAME = "accessgrab_diagnostics.log"
    private val entries = ArrayDeque<String>(MAX_ENTRIES)
    private var appContext: Context? = null

    fun init(context: Context, appVersion: String) {
        appContext = context.applicationContext
        loadPersistedLogs()
        log(
            "APP_START",
            "AccessGrab v$appVersion başlatıldı | Cihaz: ${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        )
    }

    @Synchronized
    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val errDetail = if (throwable != null) {
            val topTrace = throwable.stackTrace.take(5).joinToString(" <- ") { "${it.fileName}:${it.lineNumber}" }
            " | Hata: ${throwable.javaClass.simpleName}: ${throwable.message} [$topTrace]"
        } else {
            ""
        }
        val line = "[$timeStr] [$tag] $message$errDetail"
        if (entries.size >= MAX_ENTRIES) {
            entries.removeFirst()
        }
        entries.addLast(line)
        persistLogs()
    }

    @Synchronized
    private fun loadPersistedLogs() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.cacheDir, LOG_FILE_NAME)
            if (file.exists()) {
                val lines = file.readLines(Charsets.UTF_8).takeLast(MAX_ENTRIES)
                entries.clear()
                entries.addAll(lines)
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun persistLogs() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.cacheDir, LOG_FILE_NAME)
            file.writeText(entries.joinToString("\n"), Charsets.UTF_8)
        } catch (_: Exception) {}
    }

    @Synchronized
    fun buildFullReport(): String {
        val sb = StringBuilder()
        sb.appendLine("=== UYGULAMA İÇİ OLAY GÜNLÜĞÜ (APP EVENTS) ===")
        if (entries.isEmpty()) {
            sb.appendLine("(Henüz kayıtlı olay yok)")
        } else {
            entries.forEach { sb.appendLine(it) }
        }

        // Uygulamanın kendi sürecine ait son Logcat satırlarını ekle
        try {
            val pid = android.os.Process.myPid()
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "60", "--pid=$pid"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logcatLines = reader.readLines()
            if (logcatLines.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== SİSTEM LOGCAT (SON 60 SATIR) ===")
                logcatLines.forEach { sb.appendLine(it) }
            }
        } catch (_: Exception) {}

        return sb.toString()
    }
}
