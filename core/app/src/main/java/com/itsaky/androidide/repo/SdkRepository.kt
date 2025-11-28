package com.itsaky.androidide.repo

import com.itsaky.androidide.app.configuration.IDEBuildConfigProvider
import com.itsaky.androidide.models.sdk.SdkManifest
import com.itsaky.androidide.models.sdk.SdkPackageGroup
import com.itsaky.androidide.models.sdk.SdkPackageItem
import com.itsaky.androidide.utils.Environment
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File
import java.util.Properties
import kotlin.math.min

/**
 * @author android_zero
 * @author Akash Yadav (Early contributor)
 *
 * This file has been updated to fix compilation errors and improve package status checking logic.
 * - Added missing helper functions: getInstallationDir, getInstalledVersion, isNewerVersion.
 * - Refactored getSdkPackages to use checkPackageStatus for accurate installation and update status.
 * - Corrected logic for locating OpenJDK installation directory.
 */
interface SdkManifestApi {
    @GET("android-zeros/androidide-tools/main/manifest.json")
    suspend fun getManifest(): SdkManifest
}

object SdkRepository {

    private val api: SdkManifestApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SdkManifestApi::class.java)
    }

    suspend fun getSdkPackages(): List<SdkPackageGroup> {
        val manifest = api.getManifest()
        val packages = mutableListOf<SdkPackageGroup>()
        val arch = getCurrentDeviceArch()

        // Android SDK Platform
        val platformGroup = SdkPackageGroup(
            name = "Android SDK Platform",
            description = "包含核心的 android.jar",
            packages = emptyList() // Placeholder
        )
        packages.add(platformGroup.copy(packages = listOf(
            checkPackageStatus(platformGroup, SdkPackageItem(
                name = "Android SDK Platform",
                version = "Latest", // Platform doesn't have a specific version in this manifest
                downloadUrl = manifest.androidSdk
            ))
        )))

        // Command-line Tools
        val cmdlineGroup = SdkPackageGroup(
            name = "Android SDK Command-line Tools",
            description = "包含 apkanalyzer, avdmanager 等工具",
            packages = emptyList()
        )
        packages.add(cmdlineGroup.copy(packages = listOf(
            checkPackageStatus(cmdlineGroup, SdkPackageItem(
                name = "Command-line Tools",
                version = "Latest",
                downloadUrl = manifest.cmdlineTools
            ))
        )))

        // Build Tools
        manifest.buildTools[arch]?.let { versions ->
            val group = SdkPackageGroup(
                name = "Android SDK Build-Tools",
                description = "包含 aapt2, d8, dx 等构建工具",
                packages = emptyList()
            )
            val buildToolsItems = versions.map { (versionKey, url) ->
                val cleanVersion = versionKey.replace("_", ".")
                val item = SdkPackageItem(
                    name = cleanVersion,
                    version = cleanVersion,
                    downloadUrl = url
                )
                checkPackageStatus(group, item)
            }.sortedByDescending { it.version }
            packages.add(group.copy(packages = buildToolsItems))
        }

        // Platform Tools
        manifest.platformTools[arch]?.let { versions ->
            val group = SdkPackageGroup(
                name = "Android SDK Platform-Tools",
                description = "包含 adb, fastboot 等平台工具",
                packages = emptyList()
            )
            val platformToolsItems = versions.map { (versionKey, url) ->
                val cleanVersion = versionKey.replace("_", ".")
                val item = SdkPackageItem(
                    name = cleanVersion,
                    version = cleanVersion,
                    downloadUrl = url
                )
                checkPackageStatus(group, item)
            }.sortedByDescending { it.version }
            packages.add(group.copy(packages = platformToolsItems))
        }

        // JDK 11
        manifest.jdk11[arch]?.let { url ->
            val group = SdkPackageGroup(
                name = "OpenJDK 11",
                description = "构建Java项目所需的JDK",
                packages = emptyList()
            )
            packages.add(group.copy(packages = listOf(
                checkPackageStatus(group, SdkPackageItem(
                    name = "JDK 11 ($arch)",
                    version = "11",
                    downloadUrl = url
                ))
            )))
        }

        return packages
    }

    private fun checkPackageStatus(group: SdkPackageGroup, item: SdkPackageItem): SdkPackageItem {
        val installationDir = getInstallationDir(group, item)
        if (installationDir == null || !installationDir.exists() || !installationDir.isDirectory) {
            return item.copy(isInstalled = false, isUpdateAvailable = false)
        }

        val installedVersion = getInstalledVersion(installationDir)
        val isUpdateAvailable = isNewerVersion(item.version, installedVersion)

        return item.copy(isInstalled = true, isUpdateAvailable = isUpdateAvailable)
    }

    private fun getInstallationDir(group: SdkPackageGroup, item: SdkPackageItem): File? {
        val sdkHome = Environment.ANDROID_HOME
        return when {
            group.name == "Android SDK Platform" -> File(sdkHome, "platforms")
            group.name == "Android SDK Command-line Tools" -> File(sdkHome, "cmdline-tools/latest")
            group.name == "Android SDK Build-Tools" -> File(sdkHome, "build-tools/${item.version}")
            group.name == "Android SDK Platform-Tools" -> File(sdkHome, "platform-tools")
            group.name.startsWith("OpenJDK") -> Environment.JAVA_HOME
            else -> null
        }
    }

    private fun getInstalledVersion(installationDir: File): String? {
        val propsFile = File(installationDir, "source.properties")
        if (propsFile.exists()) {
            try {
                val props = Properties()
                propsFile.reader().use { props.load(it) }
                return props.getProperty("Pkg.Revision")
            } catch (e: Exception) {
                // Ignore and fallback
            }
        }
        // Fallback for build-tools where version is the folder name
        if (installationDir.parentFile?.name == "build-tools") {
            return installationDir.name
        }
        return null
    }

    private fun isNewerVersion(remoteVersion: String, localVersion: String?): Boolean {
        if (localVersion == null) return true // If not installed, it's always an "update" available.
        if (remoteVersion == "Latest") return false // Cannot determine update for "Latest" tags without more info.
        
        try {
            val remoteParts = remoteVersion.split('.').map { it.toInt() }
            val localParts = localVersion.split('.').map { it.toInt() }
            val commonLength = min(remoteParts.size, localParts.size)
            for (i in 0 until commonLength) {
                if (remoteParts[i] > localParts[i]) return true
                if (remoteParts[i] < localParts[i]) return false
            }
            return remoteParts.size > localParts.size
        } catch (e: NumberFormatException) {
            return false // Could not parse versions, assume not newer.
        }
    }
    
    private fun getCurrentDeviceArch(): String {
        return IDEBuildConfigProvider.getInstance().cpuArch.name.lowercase()
    }
}