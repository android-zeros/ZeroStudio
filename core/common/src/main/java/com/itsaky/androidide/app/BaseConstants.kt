package com.itsaky.androidide.app

import com.itsaky.androidide.buildinfo.BuildInfo

class BaseConstants {
    companion object {
        const val NOTIFICATION_GRADLE_BUILD_SERVICE = "17571"
        const val TELEGRAM_GROUP_URL = "https://t.me/zerostudioide"
        const val TELEGRAM_CHANNEL_URL = "https://t.me/android_zero_studio"
        const val SPONSOR_URL = "${BuildInfo.PROJECT_SITE}/donate"
        const val DOCS_URL = "https://docs.androidide.com"
        const val CONTRIBUTOR_GUIDE_URL = "${BuildInfo.REPO_URL}/blob/dev/CONTRIBUTING.md" //BuildInfo.REPO_URL + "/blob/dev/CONTRIBUTING.md"
        const val EMAIL = "msmt8382@gmail.com"
        const val QQ_Group = "https://qm.qq.com/q/y0DCfWlEbu"
        
        /** ABI: arm64-v8a (64-bit) */
        const val ABI_ARM64_V8A = "arm64-v8a"
       /** ABI: armeabi-v7a (32-bit) */
        const val ABI_ARMEABI_V7A = "armeabi-v7a"
       /** ABI: x86 (32-bit) */
        const val ABI_X86 = "x86"
       /** ABI: x86_64 (64-bit) */
        const val ABI_X86_64 = "x86_64"
        
        /* java version*/
        const val DEFAULT_JAVA_VERSION_25 = "25"
        const val DEFAULT_JAVA_VERSION_24 = "24"
        const val DEFAULT_JAVA_VERSION_23 = "23"
        const val DEFAULT_JAVA_VERSION_22 = "22"
        const val DEFAULT_JAVA_VERSION_21 = "18"
        const val DEFAULT_JAVA_VERSION_17 = "17"
        const val DEFAULT_JAVA_VERSION_13 = "13"
        const val DEFAULT_JAVA_VERSION_11 = "11"
        const val DEFAULT_JAVA_VERSION_9 = "9"
        const val DEFAULT_JAVA_VERSION_8 = "8"
        
    }
}
