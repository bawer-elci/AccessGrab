package com.baverelci.accessgrab.network

import com.google.gson.annotations.SerializedName

data class ExtractRequest(
    @SerializedName("url") val url: String = "",
    @SerializedName("key") val key: String? = null
)

data class FormatOption(
    @SerializedName("format") val format: String,
    @SerializedName("label") val label: String,
    @SerializedName("desc") val desc: String
)

data class ExtractResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("platform") val platform: String,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("duration_formatted") val durationFormatted: String,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("url") val url: String,
    @SerializedName("supported_formats") val supportedFormats: List<FormatOption>
)

data class UpdateInfo(
    @SerializedName("latest_version_code") val latestVersionCode: Int,
    @SerializedName("latest_version_name") val latestVersionName: String,
    @SerializedName("apk_url") val apkUrl: String,
    @SerializedName("release_date") val releaseDate: String,
    @SerializedName("changelog") val changelog: String,
    @SerializedName("mandatory") val mandatory: Boolean
)

data class ErrorResponse(
    @SerializedName("detail") val detail: String?
)
