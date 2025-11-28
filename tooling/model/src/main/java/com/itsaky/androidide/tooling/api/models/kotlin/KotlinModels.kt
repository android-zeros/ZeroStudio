package com.itsaky.androidide.tooling.api.models.kotlinnative

import java.io.File
import java.io.Serializable

/**
 * 纯 Kotlin/Native 或 KMP 项目的顶层元数据。
 * @author android_zero
 */
data class KotlinProjectMetadata(
    val sourceSets: List<KotlinSourceSetMetadata>,
    val targets: List<KotlinTargetMetadata>
) : Serializable

/**
 * Kotlin 源集元数据，对应 KMP 中的 "commonMain", "nativeMain" 等。
 * @author android_zero
 */
data class KotlinSourceSetMetadata(
    val name: String,
    val sourceFiles: List<File>, // .kt 文件
    val resourceFiles: List<File>, // 资源文件
    val languageSettings: KotlinLanguageSettings?
) : Serializable

/**
 * Kotlin 编译目标元数据，如 `linuxX64`, `androidNativeArm64` 等。
 * @author android_zero
 */
data class KotlinTargetMetadata(
    val name: String,
    val platformType: KotlinPlatformType,
    val compilations: List<KotlinCompilationMetadata>
) : Serializable

/**
 * Kotlin 编译单元元数据，如 `main` 或 `test`。
 * @author android_zero
 */
data class KotlinCompilationMetadata(
    val name: String,
    val compileTaskPath: String,
    val compilerOptions: KotlinCompilerOptions?,
    val output: KotlinCompilationOutput?,
    val dependencies: List<String> // 依赖的 GAV 坐标或项目路径
) : Serializable

/**
 * Kotlin 编译单元的输出信息。
 * @author android_zero
 */
data class KotlinCompilationOutput(
    val classesDirs: List<File>,
    val resourcesDir: File?
) : Serializable

/**
 * Kotlin 语言设置。
 * @author android_zero
 */
data class KotlinLanguageSettings(
    val languageVersion: String?,
    val apiVersion: String?
) : Serializable

/**
 * Kotlin 编译器选项。
 * @author android_zero
 */
data class KotlinCompilerOptions(
    val freeCompilerArgs: List<String>
) : Serializable

/**
 * Kotlin 平台类型枚举。
 * @author android_zero
 */
enum class KotlinPlatformType : Serializable {
    COMMON,
    JVM,
    JS,
    ANDROID_JVM,
    NATIVE
}