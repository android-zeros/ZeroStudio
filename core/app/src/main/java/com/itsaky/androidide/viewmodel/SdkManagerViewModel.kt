package com.itsaky.androidide.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itsaky.androidide.models.sdk.SdkPackageGroup
import com.itsaky.androidide.models.sdk.SdkPackageItem
import com.itsaky.androidide.repo.SdkRepository
import com.itsaky.androidide.services.sdk.SdkManagerService
import com.itsaky.androidide.utils.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for SdkManagerActivity.
 * It fetches SDK package information, handles user interactions like downloading or deleting packages,
 * and communicates status updates to the UI.
 *
 * @author android_zero
 */
class SdkManagerViewModel : ViewModel() {

    private val _packageGroups = MutableLiveData<List<SdkPackageGroup>>()
    val packageGroups: LiveData<List<SdkPackageGroup>> = _packageGroups

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    init {
        loadPackages()
    }

    /**
     * Fetches the list of SDK packages from the repository and updates the UI state.
     */
    fun loadPackages() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val packages = SdkRepository.getSdkPackages()
                _packageGroups.postValue(packages)
            } catch (e: Exception) {
                _packageGroups.postValue(emptyList())
                _toastMessage.postValue("加载SDK列表失败: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Toggles the expansion state of an SDK package group.
     */
    fun toggleGroupExpansion(groupName: String) {
        val currentList = _packageGroups.value ?: return
        val newList = currentList.map {
            if (it.name == groupName) {
                it.copy(isExpanded = !it.isExpanded)
            } else {
                it
            }
        }
        _packageGroups.value = newList
    }

    /**
     * Starts the SdkManagerService to download a specific SDK package.
     */
    fun startDownload(context: Context, item: SdkPackageItem) {
        val intent = Intent(context, SdkManagerService::class.java).apply {
            putExtra(SdkManagerService.EXTRA_URL, item.downloadUrl)
            putExtra(SdkManagerService.EXTRA_PACKAGE_NAME, item.name)

            // Determine the correct destination subpath based on the package type.
            val destSubPath = when {
                item.name.contains("Build-Tools", ignoreCase = true) -> "build-tools/${item.version}"
                item.name.contains("Platform-Tools", ignoreCase = true) -> "platform-tools"
                item.name.contains("Platform", ignoreCase = true) -> "." // Extract to sdk root, it contains 'platforms' dir
                item.name.contains("Command-line Tools", ignoreCase = true) -> "cmdline-tools"
                item.name.contains("JDK", ignoreCase = true) -> Environment.JAVA_HOME.absolutePath // Use absolute path for JDK
                else -> "temp/${item.name}" // Fallback for unknown types
            }
            putExtra(SdkManagerService.EXTRA_DEST_SUBPATH, destSubPath)
        }
        context.startService(intent)
    }

    /**
     * Shows a confirmation dialog and then deletes an SDK package if confirmed.
     */
    fun deletePackage(context: Context, item: SdkPackageItem) {
        MaterialAlertDialogBuilder(context)
            .setTitle("确认删除")
            .setMessage("您确定要删除 ${item.name} 吗？此操作不可撤销。")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                executeDeletion(item)
            }
            .show()
    }

    /**
     * Performs the actual file deletion in a background coroutine.
     */
    private fun executeDeletion(item: SdkPackageItem) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _isLoading.value = true
            }
            try {
                val fileToDelete = when {
                    item.name.contains("Build-Tools", ignoreCase = true) -> File(Environment.ANDROID_HOME, "build-tools/${item.version}")
                    item.name.contains("Platform-Tools", ignoreCase = true) -> File(Environment.ANDROID_HOME, "platform-tools")
                    // item.name.contains("Platform", ignoreCase = true) -> File(Environment.ANDROID_HOME, "platforms")
                    item.name.contains("Platform") -> File(Environment.ANDROID_HOME, "platforms/android-${item.version}")
                    item.name.contains("Command-line Tools", ignoreCase = true) -> File(Environment.ANDROID_HOME, "cmdline-tools")
                    item.name.contains("JDK", ignoreCase = true) -> Environment.JAVA_HOME
                    else -> null
                }

                if (fileToDelete != null && fileToDelete.exists()) {
                    if (fileToDelete.deleteRecursively()) {
                        _toastMessage.postValue("${item.name} 删除成功")
                    } else {
                        _toastMessage.postValue("${item.name} 删除失败")
                    }
                } else {
                    _toastMessage.postValue("文件不存在，无法删除")
                }
            } catch (e: Exception) {
                _toastMessage.postValue("删除时发生错误: ${e.message}")
            } finally {
                // Refresh the list to show the updated state
                loadPackages()
            }
        }
    }
    
    /**
     * Handles status updates broadcasted from SdkManagerService and updates the UI accordingly.
     */
    fun handleBroadcastUpdate(url: String, status: Int, progress: Int, error: String?) {
        val currentList = _packageGroups.value ?: return
        val newList = currentList.map { group ->
            group.copy(packages = group.packages.map { pkg ->
                if (pkg.downloadUrl == url) {
                    when (status) {
                        SdkManagerService.STATUS_STARTED, SdkManagerService.STATUS_EXTRACTING -> pkg.copy(isDownloading = true, downloadProgress = -1) // Indeterminate
                        SdkManagerService.STATUS_PROGRESS -> pkg.copy(isDownloading = true, downloadProgress = progress)
                        SdkManagerService.STATUS_COMPLETED -> {
                            // On completion, we reload the whole list to ensure correct installed status
                            loadPackages()
                            _toastMessage.postValue("${pkg.name} 安装成功")
                            pkg.copy(isDownloading = false, isInstalled = true, downloadProgress = 0)
                        }
                        SdkManagerService.STATUS_FAILED -> {
                            _toastMessage.postValue("${pkg.name} 安装失败: $error")
                            pkg.copy(isDownloading = false, isInstalled = false, downloadProgress = 0)
                        }
                        else -> pkg
                    }
                } else {
                    pkg
                }
            })
        }
        _packageGroups.postValue(newList)
    }

    /**
     * Resets the toast message LiveData to null after it has been shown.
     */
    fun onToastShown() {
        _toastMessage.value = null
    }
}