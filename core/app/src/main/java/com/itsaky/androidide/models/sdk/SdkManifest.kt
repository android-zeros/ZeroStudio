package com.itsaky.androidide.models.sdk

import com.google.gson.annotations.SerializedName

/**
 * Mirrors the structure of manifest.json
 */
data class SdkManifest(
    @SerializedName("android_sdk")
    val androidSdk: String,
    @SerializedName("cmdline_tools")
    val cmdlineTools: String,
    @SerializedName("build_tools")
    val buildTools: Map<String, Map<String, String>>, // arch -> version -> url
    @SerializedName("platform_tools")
    val platformTools: Map<String, Map<String, String>>, // arch -> version -> url
    @SerializedName("jdk_11")
    val jdk11: Map<String, String> // arch -> url
)