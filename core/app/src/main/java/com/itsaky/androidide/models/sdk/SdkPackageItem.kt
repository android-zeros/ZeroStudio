package com.itsaky.androidide.models.sdk

import com.google.gson.annotations.SerializedName

/**
 * 代表一个可安装的SDK包条目，如 build-tools 34.0.4
 *
 * @author android_zero
 */
data class SdkPackageItem(
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String,
    @SerializedName("url") val downloadUrl: String,
    val isInstalled: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val isUpdateAvailable: Boolean = false
)