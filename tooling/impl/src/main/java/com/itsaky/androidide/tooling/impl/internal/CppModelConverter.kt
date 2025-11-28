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
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.tooling.impl.internal

import com.itsaky.androidide.tooling.api.models.GradleTask
import com.itsaky.androidide.tooling.api.models.nativecpp.*
import org.gradle.tooling.model.GradleTask as ToolingGradleTask // Use an alias to avoid name clash
import org.gradle.tooling.model.cpp.*

/**
 * 将官方 Gradle C++ 模型转换为 AndroidIDE 自定义的 DTO 模型。
 * @author android_zero
 */

fun CppProject.toMetadata(): CppProjectMetadata {
    return CppProjectMetadata(
        mainComponent = this.mainComponent?.toMetadata(),
        testComponent = this.testComponent?.toMetadata() as? CppTestSuiteMetadata
    )
}

fun CppComponent.toMetadata(): CppComponentMetadata {
    return when (this) {
        is CppApplication -> CppApplicationMetadata(
            name = this.name,
            baseName = this.baseName,
            binaries = this.binaries.map { it.toMetadata() }
        )
        is CppLibrary -> CppLibraryMetadata(
            name = this.name,
            baseName = this.baseName,
            binaries = this.binaries.map { it.toMetadata() }
        )
        is CppTestSuite -> CppTestSuiteMetadata(
            name = this.name,
            baseName = this.baseName,
            binaries = this.binaries.map { it.toMetadata() }
        )
        else -> throw IllegalArgumentException("Unknown CppComponent type: ${this.javaClass}")
    }
}

fun CppBinary.toMetadata(): CppBinaryMetadata {
    val compilationDetailsMeta = this.compilationDetails?.toMetadata()
    val linkageDetailsMeta = this.linkageDetails?.toMetadata()
    return when (this) {
        is CppExecutable -> CppExecutableMetadata(
            name = this.name,
            variantName = this.variantName,
            baseName = this.baseName,
            compilationDetails = compilationDetailsMeta,
            linkageDetails = linkageDetailsMeta
        )
        is CppSharedLibrary -> CppSharedLibraryMetadata(
            name = this.name,
            variantName = this.variantName,
            baseName = this.baseName,
            compilationDetails = compilationDetailsMeta,
            linkageDetails = linkageDetailsMeta
        )
        is CppStaticLibrary -> CppStaticLibraryMetadata(
            name = this.name,
            variantName = this.variantName,
            baseName = this.baseName,
            compilationDetails = compilationDetailsMeta,
            linkageDetails = linkageDetailsMeta
        )
        else -> throw IllegalArgumentException("Unknown CppBinary type: ${this.javaClass}")
    }
}

fun CompilationDetails.toMetadata(): CompilationDetailsMetadata {
    return CompilationDetailsMetadata(
        compileTask = (this.compileTask as? ToolingGradleTask)?.toMetadata(),
        compilerExecutable = this.compilerExecutable,
        compileWorkingDir = this.compileWorkingDir,
        frameworkSearchPaths = this.frameworkSearchPaths,
        systemHeaderSearchPaths = this.systemHeaderSearchPaths,
        userHeaderSearchPaths = this.userHeaderSearchPaths,
        sources = this.sources.map { it.toMetadata() },
        headerDirs = this.headerDirs.toList(),
        macroDefines = this.macroDefines.map { it.toMetadata() },
        macroUndefines = this.macroUndefines.toList(),
        additionalArgs = this.additionalArgs
    )
}

fun LinkageDetails.toMetadata(): LinkageDetailsMetadata {
    return LinkageDetailsMetadata(
        linkTask = (this.linkTask as? ToolingGradleTask)?.toMetadata(),
        outputLocation = this.outputLocation,
        additionalArgs = this.additionalArgs
    )
}

fun SourceFile.toMetadata(): SourceFileMetadata {
    return SourceFileMetadata(
        sourceFile = this.sourceFile,
        objectFile = this.objectFile
    )
}

fun MacroDirective.toMetadata(): MacroDirectiveMetadata {
    return MacroDirectiveMetadata(
        name = this.name,
        value = this.value
    )
}

fun ToolingGradleTask.toMetadata(): GradleTask {
    return GradleTask(
        name = this.name,
        description = this.description,
        group = try { this.group } catch (e: Exception) { null },
        path = this.path,
        displayName = this.displayName,
        isPublic = this.isPublic,
        projectPath = this.project.path
    )
}