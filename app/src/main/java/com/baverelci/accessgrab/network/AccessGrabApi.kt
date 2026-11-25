package com.baverelci.accessgrab.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AccessGrabService {

    @POST("api/extract")
    suspend fun extractMedia(
        @Body request: ExtractRequest,
        @Header("X-AccessGrab-Key") apiKey: String = AccessGrabApi.API_KEY
    ): ExtractResponse

    @POST("api/search")
    suspend fun searchMedia(
        @Body request: SearchRequest,
        @Header("X-AccessGrab-Key") apiKey: String = AccessGrabApi.API_KEY
    ): SearchResponse

    @GET("api/version")
    suspend fun getLatestVersion(): UpdateInfo

    @POST("api/report-log")
    suspend fun reportClientLog(
        @Body request: LogReportRequest,
        @Header("X-AccessGrab-Key") apiKey: String = AccessGrabApi.API_KEY
    ): LogReportResponse

    @GET("api/announcement")
    suspend fun getAnnouncement(): AnnouncementInfo

    @POST("api/feedback")
    suspend fun sendFeedback(
        @Body request: FeedbackRequest,
        @Header("X-AccessGrab-Key") apiKey: String = AccessGrabApi.API_KEY
    ): FeedbackResponse

    @POST("api/playlist")
    suspend fun extractPlaylist(
        @Body request: ExtractRequest,
        @Header("X-AccessGrab-Key") apiKey: String = AccessGrabApi.API_KEY
    ): SearchResponse

    @POST("api/transcript")
    suspend fun getTranscript(
        @Body request: ExtractRequest,
        @Header("X-AccessGrab-Key") apiKey: String = AccessGrabApi.API_KEY
    ): TranscriptResponse
}

object AccessGrabApi {
    const val BASE_URL = "https://baverelci.com.tr/accessgrab/"
    const val API_KEY = "ag_baver_accessgrab_2026_key"

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: AccessGrabService by lazy {
        retrofit.create(AccessGrabService::class.java)
    }

    fun buildDownloadUrl(mediaUrl: String, format: String, quality: String? = null): String {
        val encodedUrl = java.net.URLEncoder.encode(mediaUrl, "UTF-8")
        val qualityParam = if (!quality.isNullOrBlank()) "&quality=$quality" else ""
        return "${BASE_URL}api/download?url=$encodedUrl&format=$format$qualityParam&key=$API_KEY"
    }

    fun buildPreviewUrl(mediaUrl: String): String {
        val encodedUrl = java.net.URLEncoder.encode(mediaUrl, "UTF-8")
        return "${BASE_URL}api/preview?url=$encodedUrl&key=$API_KEY"
    }
}
