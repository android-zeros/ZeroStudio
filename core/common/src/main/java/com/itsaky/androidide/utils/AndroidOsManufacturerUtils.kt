package com.itsaky.androidide.utils

import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * 一个用于识别设备制造商和定制OS的工具类。
 *
 * @author android_zero
 */
object AndroidOsManufacturerUtils {

    /**
     * 定义了已知的设备品牌或定制OS。
     */
    enum class DeviceBrand {
        XIAOMI,   // 小米 (MIUI / HyperOS)
        SAMSUNG,  // 三星 (One UI)
        MEIZU,    // 魅族 (Flyme)
        OPPO,     // OPPO (ColorOS)
        VIVO,     // vivo (Funtouch OS / OriginOS)
        HUAWEI,   // 华为 (EMUI / HarmonyOS)
        OTHER     // 其他或未知
    }

    /**
     * 获取当前设备的品牌/OS，结果会被懒加载并缓存。
     *
     * 首次访问此属性时会执行检测逻辑，后续访问将直接返回缓存结果。
     * @return [DeviceBrand] 枚举，代表当前设备品牌。
     */
    val brand: DeviceBrand by lazy {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        when {
            manufacturer == "xiaomi" || getSystemProperty("ro.miui.ui.version.name").isNotEmpty() -> DeviceBrand.XIAOMI
            manufacturer == "samsung" -> DeviceBrand.SAMSUNG
            manufacturer == "meizu" || getSystemProperty("ro.build.flyme.version").isNotEmpty() -> DeviceBrand.MEIZU
            manufacturer == "oppo" || getSystemProperty("ro.build.version.opporom").isNotEmpty() -> DeviceBrand.OPPO
            manufacturer == "vivo" || getSystemProperty("ro.vivo.os.version").isNotEmpty() -> DeviceBrand.VIVO
            manufacturer == "huawei" || getSystemProperty("ro.build.version.emui").isNotEmpty() -> DeviceBrand.HUAWEI
            else -> DeviceBrand.OTHER
        }
    }

    /**
     * 通过执行 'getprop' 命令来读取系统属性
     * @param key 要查询的系统属性键。
     * @return 属性值，如果不存在或发生错误则返回空字符串。
     */
    private fun getSystemProperty(key: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readLine() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}