package com.itsaky.androidide.models.sdk

/**
 * 代表一个SDK包的分组，如 "Build-Tools"
 * 这是一个可在RecyclerView中展开和折叠的父项
*
*@author android_zero
 */
data class SdkPackageGroup(
    val name: String,
    val description: String,
    val packages: List<SdkPackageItem>,
    var isExpanded: Boolean = false
)