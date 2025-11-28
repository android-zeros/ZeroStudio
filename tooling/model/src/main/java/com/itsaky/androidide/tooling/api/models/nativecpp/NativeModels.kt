package com.itsaky.androidide.tooling.api.models.nativecpp

import com.itsaky.androidide.tooling.api.models.GradleTask
import com.itsaky.androidide.tooling.api.models.ProjectMetadata
import java.io.File
import java.io.Serializable

/**
 * C++ 项目的顶层元数据，镜像自 `org.gradle.tooling.model.cpp.CppProject`。
 * @author android_zero
 */
data class CppProjectMetadata(
    val mainComponent: CppComponentMetadata?,
    val testComponent: CppTestSuiteMetadata?
) : Serializable

/**
 * 代表一个 C++ 组件（如一个应用或库），镜像自 `org.gradle.tooling.model.cpp.CppComponent`。
 * @author android_zero
 */
interface CppComponentMetadata : Serializable {
    val name: String
    val baseName: String
    val binaries: List<CppBinaryMetadata>
}

/**
 * C++ 应用程序组件，镜像自 `org.gradle.tooling.model.cpp.CppApplication`。
 * @author android_zero
 */
data class CppApplicationMetadata(
    override val name: String,
    override val baseName: String,
    override val binaries: List<CppBinaryMetadata>
) : CppComponentMetadata

/**
 * C++ 库组件，镜像自 `org.gradle.tooling.model.cpp.CppLibrary`。
 * @author android_zero
 */
data class CppLibraryMetadata(
    override val name: String,
    override val baseName: String,
    override val binaries: List<CppBinaryMetadata>
) : CppComponentMetadata

/**
 * C++ 测试套件组件，镜像自 `org.gradle.tooling.model.cpp.CppTestSuite`。
 * @author android_zero
 */
data class CppTestSuiteMetadata(
    override val name: String,
    override val baseName: String,
    override val binaries: List<CppBinaryMetadata>
) : CppComponentMetadata

/**
 * 代表一个 C++ 构建产物（如可执行文件或库文件），镜像自 `org.gradle.tooling.model.cpp.CppBinary`。
 * @author android_zero
 */
interface CppBinaryMetadata : Serializable {
    val name: String
    val variantName: String
    val baseName: String
    val compilationDetails: CompilationDetailsMetadata?
    val linkageDetails: LinkageDetailsMetadata?
}

/**
 * C++ 可执行文件，镜像自 `org.gradle.tooling.model.cpp.CppExecutable`。
 * @author android_zero
 */
data class CppExecutableMetadata(
    override val name: String,
    override val variantName: String,
    override val baseName: String,
    override val compilationDetails: CompilationDetailsMetadata?,
    override val linkageDetails: LinkageDetailsMetadata?
) : CppBinaryMetadata

/**
 * C++ 共享库，镜像自 `org.gradle.tooling.model.cpp.CppSharedLibrary`。
 * @author android_zero
 */
data class CppSharedLibraryMetadata(
    override val name: String,
    override val variantName: String,
    override val baseName: String,
    override val compilationDetails: CompilationDetailsMetadata?,
    override val linkageDetails: LinkageDetailsMetadata?
) : CppBinaryMetadata

/**
 * C++ 静态库，镜像自 `org.gradle.tooling.model.cpp.CppStaticLibrary`。
 * @author android_zero
 */
data class CppStaticLibraryMetadata(
    override val name: String,
    override val variantName: String,
    override val baseName: String,
    override val compilationDetails: CompilationDetailsMetadata?,
    override val linkageDetails: LinkageDetailsMetadata?
) : CppBinaryMetadata

/**
 * 编译细节，镜像自 `org.gradle.tooling.model.cpp.CompilationDetails`。
 * @author android_zero
 */
data class CompilationDetailsMetadata(
    val compileTask: GradleTask?,
    val compilerExecutable: File?,
    val compileWorkingDir: File?,
    val frameworkSearchPaths: List<File>,
    val systemHeaderSearchPaths: List<File>,
    val userHeaderSearchPaths: List<File>,
    val sources: List<SourceFileMetadata>,
    val headerDirs: List<File>,
    val macroDefines: List<MacroDirectiveMetadata>,
    val macroUndefines: List<String>,
    val additionalArgs: List<String>
) : Serializable

/**
 * 链接细节，镜像自 `org.gradle.tooling.model.cpp.LinkageDetails`。
 * @author android_zero
 */
data class LinkageDetailsMetadata(
    val linkTask: GradleTask?,
    val outputLocation: File?,
    val additionalArgs: List<String>
) : Serializable

/**
 * 源文件信息，镜像自 `org.gradle.tooling.model.cpp.SourceFile`。
 * @author android_zero
 */
data class SourceFileMetadata(
    val sourceFile: File,
    val objectFile: File?
) : Serializable

/**
 * 宏指令，镜像自 `org.gradle.tooling.model.cpp.MacroDirective`。
 * @author android_zero
 */
data class MacroDirectiveMetadata(
    val name: String,
    val value: String?
) : Serializable