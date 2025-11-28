/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.fragments.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.appintro.SlidePolicy
import com.itsaky.androidide.R
import com.itsaky.androidide.adapters.onboarding.OnboardingPermissionsAdapter
import com.itsaky.androidide.databinding.FragmentOnboardingPermissionsBinding
import com.itsaky.androidide.fragments.FragmentWithBinding
import com.itsaky.androidide.models.OnboardingPermissionItem
import com.itsaky.androidide.utils.AndroidOsManufacturerUtils
import com.itsaky.androidide.utils.flashError
import com.itsaky.androidide.utils.isAtLeastO
import com.itsaky.androidide.utils.isAtLeastR
import com.itsaky.androidide.utils.isAtLeastT
import java.util.LinkedList
import java.util.Queue

/**
 * This is a permission request fragment
 * 增加系统os特性适配，增加一个一键申请全部权限按钮
 *
 * @author Akash Yadav
 * @author android_zero 
 */
class PermissionsFragment : FragmentWithBinding<FragmentOnboardingPermissionsBinding>(FragmentOnboardingPermissionsBinding::inflate), SlidePolicy {

    private var adapter: OnboardingPermissionsAdapter? = null
    private val permissionRequestQueue: Queue<OnboardingPermissionItem> = LinkedList()
    private val TAG = "PermissionsFragment"

    // 标准权限申请启动器（处理多权限申请）
    private val standardPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedMap ->
        onPermissionsUpdated()
        processNextPermissionInQueue()
        // 针对通知权限：若拒绝，自动引导至设置页
        grantedMap.forEach { (permission, isGranted) ->
            if (permission == PERMISSION_TYPE_NOTIFICATIONS && !isGranted) {
                launchSettingsIntent(permission)
            }
        }
    }

    // 设置页跳转启动器（处理需手动开启的权限）
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onPermissionsUpdated()
        processNextPermissionInQueue()
    }

    // 懒加载所需权限列表
    private val permissions by lazy { getRequiredPermissions(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 加载标题和副标题
        binding.onboardingTitle.text = requireArguments().getCharSequence(OnboardingFragment.KEY_ONBOARDING_TITLE)
        binding.onboardingSubtitle.text = requireArguments().getCharSequence(OnboardingFragment.KEY_ONBOARDING_SUBTITLE)

        // 初始化RecyclerView（权限列表）
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = createAdapter()

        // 一键申请全部权限按钮点击事件
        binding.grantAllButton.setOnClickListener { startSequentialPermissionRequest() }
    }

    /**
     * 启动权限顺序申请（队列模式，避免同时弹出多个弹窗）
     */
    private fun startSequentialPermissionRequest() {
        binding.grantAllButton.isEnabled = false // 禁用按钮，防止重复点击
        permissionRequestQueue.clear() // 清空历史队列
        // 添加未授权的权限到队列
        permissionRequestQueue.addAll(permissions.filter { !it.isGranted })
        processNextPermissionInQueue() // 处理队列首个权限
    }

    /**
     * 处理队列中的下一个权限申请
     */
    private fun processNextPermissionInQueue() {
        val permissionItem = permissionRequestQueue.poll()
        if (permissionItem == null) {
            // 队列空：重新启用按钮（若仍有未授权权限）
            binding.grantAllButton.isEnabled = permissions.any { !it.isGranted }
            onPermissionsUpdated()
            return
        }
        // 申请当前权限
        requestPermission(permissionItem.permission)
    }

    /**
     * 申请指定权限（按权限类型适配不同逻辑）
     */
    private fun requestPermission(permission: String) {
        when (permission) {
            // 通知权限：Android 13+弹窗申请，低版本直接跳转设置
            PERMISSION_TYPE_NOTIFICATIONS -> {
                if (isAtLeastT()) {
                    standardPermissionsLauncher.launch(arrayOf(permission))
                } else {
                    launchSettingsIntent(permission)
                }
            }

            // 存储权限：Android 11+跳转"所有文件访问"设置，低版本申请读写权限
            PERMISSION_TYPE_STORAGE -> {
                if (isAtLeastR()) {
                    launchSettingsIntent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                } else {
                    standardPermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            }

            // 安装未知来源权限：跳转系统设置
            PERMISSION_TYPE_INSTALL_PACKAGES -> {
                launchSettingsIntent(permission)
            }

            // 其他权限：默认弹窗申请
            else -> standardPermissionsLauncher.launch(arrayOf(permission))
        }
    }

    /**
     * 跳转至权限对应的设置页（适配厂商定制系统）
     */
    private fun launchSettingsIntent(permission: String) {
        val intents = createSettingsIntents(requireContext(), permission)
        var launched = false

        // 尝试启动意图（优先厂商定制意图，其次系统意图）
        for (intent in intents) {
            try {
                if (isIntentResolvable(intent)) {
                    settingsLauncher.launch(intent)
                    launched = true
                    break
                }
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "无法解析意图：${intent.action}", e)
            }
        }

        // 意图启动失败：提示用户
        if (!launched) {
            Log.e(TAG, "无法打开权限设置页，权限类型：$permission，包名：${requireContext().packageName}")
            activity?.flashError(R.string.msg_cant_open_settings)
            processNextPermissionInQueue()
        }
    }

    /**
     * 检查意图是否可解析（避免崩溃）
     */
    private fun isIntentResolvable(intent: Intent): Boolean {
        return requireContext().packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ) != null
    }

    /**
     * 创建权限列表适配器
     */
    private fun createAdapter(): RecyclerView.Adapter<*> {
        return OnboardingPermissionsAdapter(permissions) { permission ->
            // 单个权限点击：触发申请
            requestPermission(permission)
        }.also { this.adapter = it }
    }

    /**
     * 更新权限状态（强制刷新，避免缓存）
     */
    private fun onPermissionsUpdated() {
        permissions.forEach { permissionItem ->
            // 基础权限状态查询
            permissionItem.isGranted = isPermissionGranted(requireContext(), permissionItem.permission)
            // 通知权限额外校验
            if (permissionItem.permission == PERMISSION_TYPE_NOTIFICATIONS) {
                permissionItem.isGranted = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
            }
        }
        // 刷新列表显示
        adapter?.notifyDataSetChanged()
        // 更新一键申请按钮状态
        if (permissionRequestQueue.isEmpty()) {
            binding.grantAllButton.isEnabled = permissions.any { !it.isGranted }
        }
    }

    /**
     * SlidePolicy接口：判断是否允许进入下一页（所有权限已授权）
     */
    override val isPolicyRespected: Boolean get() = permissions.all { it.isGranted }

    /**
     * SlidePolicy接口：未满足权限要求时的提示
     */
    override fun onUserIllegallyRequestedNextPage() {
        activity?.flashError(R.string.msg_grant_permissions)
    }

    companion object {
        // 权限类型常量
        private const val PERMISSION_TYPE_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS
        private const val PERMISSION_TYPE_STORAGE = "com.itsaky.androidide.permission.STORAGE"
        private const val PERMISSION_TYPE_INSTALL_PACKAGES = Manifest.permission.REQUEST_INSTALL_PACKAGES

        /**
         * 创建PermissionsFragment实例
         */
        @JvmStatic
        fun newInstance(context: Context): PermissionsFragment {
            return PermissionsFragment().apply {
                arguments = Bundle().apply {
                    putCharSequence(OnboardingFragment.KEY_ONBOARDING_TITLE, context.getString(R.string.onboarding_title_permissions))
                    putCharSequence(OnboardingFragment.KEY_ONBOARDING_SUBTITLE, context.getString(R.string.onboarding_subtitle_permissions))
                }
            }
        }

        /**
         * 获取当前设备所需的权限列表（按系统版本适配）
         */
        @JvmStatic
        fun getRequiredPermissions(context: Context): List<OnboardingPermissionItem> {
            val permissions = mutableListOf<OnboardingPermissionItem>()
            
            // 通知权限（Android 13+）
            if (isAtLeastT()) {
                permissions.add(
                  OnboardingPermissionItem(
                      permission = PERMISSION_TYPE_NOTIFICATIONS,
                      title = R.string.permission_title_notifications,
                      description = R.string.permission_desc_notifications,
                      isGranted = isPermissionGranted(context, PERMISSION_TYPE_NOTIFICATIONS)
                  )
               )
            }

            // 存储权限（Android 11+：所有文件访问；低版本：读写权限）
            val storagePermissionGranted = isPermissionGranted(context, PERMISSION_TYPE_STORAGE)
            if (isAtLeastR()) {
                permissions.add(
                    OnboardingPermissionItem(
                        permission = PERMISSION_TYPE_STORAGE,
                        title = R.string.permission_title_storage_all_files,
                        description = R.string.permission_desc_storage_all_files,
                        isGranted = storagePermissionGranted
                    )
                )
            } else {
                permissions.add(
                    OnboardingPermissionItem(
                        permission = PERMISSION_TYPE_STORAGE,
                        title = R.string.permission_title_storage,
                        description = R.string.permission_desc_storage,
                        isGranted = storagePermissionGranted
                    )
                )
            }

            // 安装未知来源权限（Android 8.0+）
            if (isAtLeastO()) {
                permissions.add(
                    OnboardingPermissionItem(
                        permission = PERMISSION_TYPE_INSTALL_PACKAGES,
                        title = R.string.permission_title_install_packages,
                        description = R.string.permission_desc_install_packages,
                        isGranted = isPermissionGranted(context, PERMISSION_TYPE_INSTALL_PACKAGES)
                    )
                )
            }

            return permissions
        }

        /**
         * 检查所有权限是否已授权
         */
        @JvmStatic
        fun areAllPermissionsGranted(context: Context): Boolean = getRequiredPermissions(context).all { it.isGranted }

        /**
         * 检查单个权限是否已授权（按权限类型适配逻辑）
         */
        @JvmStatic
        fun isPermissionGranted(context: Context, permission: String): Boolean {
            return when (permission) {
                // 通知权限：Android 13+校验通知管理器，低版本默认授权
                PERMISSION_TYPE_NOTIFICATIONS ->
                    if (isAtLeastT()) NotificationManagerCompat.from(context).areNotificationsEnabled() else true

                // 存储权限：Android 11+校验"所有文件访问"，低版本校验读写权限
                PERMISSION_TYPE_STORAGE ->
                    if (isAtLeastR()) Environment.isExternalStorageManager()
                    else checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)

                // 安装未知来源权限：Android 8.0+校验系统开关，低版本默认授权
                PERMISSION_TYPE_INSTALL_PACKAGES ->
                    if (isAtLeastO()) context.packageManager.canRequestPackageInstalls() else true

                // 其他权限：默认校验运行时权限
                else -> checkSelfPermission(context, permission)
            }
        }

        /**
         * 基础权限校验（封装系统API）
         */
        @JvmStatic
        private fun checkSelfPermission(context: Context, permission: String): Boolean {
            return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * 创建权限对应的设置意图（适配厂商定制系统）
         */
        @JvmStatic
        private fun createSettingsIntents(context: Context, permission: String): List<Intent> {
            val intents = mutableListOf<Intent>()
            val packageName = context.packageName
            val packageUri = Uri.fromParts("package", packageName, null)

            when (permission) {
                // 通知权限设置意图（适配厂商+系统）
                PERMISSION_TYPE_NOTIFICATIONS -> {
                    if (isAtLeastT()) {
                        // 厂商定制设置页（优先）
                        when (AndroidOsManufacturerUtils.brand) {
                            AndroidOsManufacturerUtils.DeviceBrand.XIAOMI -> {
                                intents.add(Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                                    setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
                                    putExtra("extra_pkgname", packageName)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                            AndroidOsManufacturerUtils.DeviceBrand.HUAWEI -> {
                                intents.add(Intent().apply {
                                    component = ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity")
                                    putExtra("packageName", packageName)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                            AndroidOsManufacturerUtils.DeviceBrand.OPPO -> {
                                intents.add(Intent().apply {
                                    component = ComponentName("com.color.safecenter", "com.color.safecenter.permission.PermissionManagerActivity")
                                    putExtra("pkg_name", packageName)
                                    putExtra("app_name", context.getString(R.string.app_name))
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                            AndroidOsManufacturerUtils.DeviceBrand.VIVO -> {
                                intents.add(Intent().apply {
                                    component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity")
                                    action = "secure.intent.action.softPermissionDetail"
                                    putExtra("packagename", packageName)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                            else -> {}
                        }
                        // 系统默认通知设置页（兜底）
                        intents.add(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            putExtra(Settings.EXTRA_CHANNEL_ID, packageName) // 精准定位应用通知设置
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } else {
                        //跳转应用详情页（引导手动开启通知）
                        intents.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }

                // 所有文件访问权限设置意图（Android 11+）
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -> {
                    if (isAtLeastR()) {
                        intents.add(Intent(permission, packageUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }

                // 安装未知来源权限设置意图（Android 8.0+）
                PERMISSION_TYPE_INSTALL_PACKAGES -> {
                    if (isAtLeastO()) {
                        intents.add(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }
            }
            return intents
        }
    }
}