package com.baverelci.accessgrab.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.baverelci.accessgrab.network.AccessGrabApi

object DownloadHelper {

    fun startDownload(
        context: Context,
        mediaUrl: String,
        format: String,
        title: String,
        quality: String? = null
    ): Long {
        val downloadUrl = AccessGrabApi.buildDownloadUrl(mediaUrl, format, quality)
        val ext = if (format.lowercase() == "mp3") "mp3" else "mp4"
        val mimeType = if (format.lowercase() == "mp3") "audio/mpeg" else "video/mp4"

        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ\\s_-]"), "")
            .trim()
            .take(60)
            .ifEmpty { "AccessGrab_Media" }

        val fileName = "$sanitizedTitle.$ext"

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle(fileName)
            setDescription("AccessGrab ile indiriliyor ($ext)")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AccessGrab/$fileName")
            setMimeType(mimeType)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }
}
