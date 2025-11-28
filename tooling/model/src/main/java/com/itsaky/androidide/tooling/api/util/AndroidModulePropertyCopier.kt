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

package com.itsaky.androidide.tooling.api.util

import com.android.builder.model.v2.CustomSourceDirectory
import com.android.builder.model.v2.ide.AndroidGradlePluginProjectFlags
import com.android.builder.model.v2.ide.AndroidGradlePluginProjectFlags.BooleanFlag
import com.android.builder.model.v2.ide.AndroidLibraryData
import com.android.builder.model.v2.ide.ApiVersion
import com.android.builder.model.v2.ide.ArtifactDependencies
import com.android.builder.model.v2.ide.GraphItem
import com.android.builder.model.v2.ide.JavaCompileOptions
import com.android.builder.model.v2.ide.Library
import com.android.builder.model.v2.ide.LibraryInfo
import com.android.builder.model.v2.ide.ProjectInfo
import com.android.builder.model.v2.ide.SourceProvider
import com.android.builder.model.v2.ide.SourceSetContainer
import com.android.builder.model.v2.ide.SyncIssue
import com.android.builder.model.v2.ide.UnresolvedDependency
import com.android.builder.model.v2.ide.ViewBindingOptions
import com.android.builder.model.v2.models.ProjectSyncIssues
import com.itsaky.androidide.builder.model.DefaultAndroidGradlePluginProjectFlags
import com.itsaky.androidide.builder.model.DefaultAndroidLibraryData
import com.itsaky.androidide.builder.model.DefaultApiVersion
import com.itsaky.androidide.builder.model.DefaultArtifactDependencies
import com.itsaky.androidide.builder.model.DefaultCustomSourceDirectory
import com.itsaky.androidide.builder.model.DefaultGraphItem
import com.itsaky.androidide.builder.model.DefaultJavaCompileOptions
import com.itsaky.androidide.builder.model.DefaultLibrary
import com.itsaky.androidide.builder.model.DefaultLibraryInfo
import com.itsaky.androidide.builder.model.DefaultProjectInfo
import com.itsaky.androidide.builder.model.DefaultProjectSyncIssues
import com.itsaky.androidide.builder.model.DefaultSourceProvider
import com.itsaky.androidide.builder.model.DefaultSourceSetContainer
import com.itsaky.androidide.builder.model.DefaultSyncIssue
import com.itsaky.androidide.builder.model.DefaultUnresolvedDependency
import com.itsaky.androidide.builder.model.DefaultViewBindingOptions
import java.io.File

/**
 * As the data is sent over streams, and the instances of properties specified in the builder-model
 * are just proxy classes, we need to make a copy of those properties so that they can be serialized
 * by Gson.
 *
 * This class handles the work of making a copy of those properties.
 *
 * @author android_zero
 * 
 * AGP 9.0 UPG: This file has been heavily modified to adapt to the new builder-model interfaces.
 * - `copy(flags)` now uses `getFlagValue(flag.name)` for better compatibility.
 * - `copy(container)` now adapts from `deviceTestSourceProviders` and `hostTestSourceProviders` maps.
 * - `copy(provider)` adds support for `baselineProfileDirectories`.
 * - `copy(value: Library)` now safely handles `srcJars` with fallback for older AGP versions.
 * - `copy(artifact)` handles nullable `runtimeDependencies`.
 */
object AndroidModulePropertyCopier {

  fun copy(viewBindingOptions: ViewBindingOptions?): DefaultViewBindingOptions? {
    return viewBindingOptions?.let { DefaultViewBindingOptions().apply { isEnabled = it.isEnabled } }
  }

  fun copy(version: ApiVersion?): DefaultApiVersion? =
    version?.let {
      DefaultApiVersion().apply {
        apiLevel = it.apiLevel
        codename = it.codename
      }
    }

  fun copy(javaCompileOptions: JavaCompileOptions): DefaultJavaCompileOptions {
    return DefaultJavaCompileOptions().apply {
      encoding = javaCompileOptions.encoding
      isCoreLibraryDesugaringEnabled = javaCompileOptions.isCoreLibraryDesugaringEnabled
      sourceCompatibility = javaCompileOptions.sourceCompatibility
      targetCompatibility = javaCompileOptions.targetCompatibility
    }
  }

  fun copy(flags: AndroidGradlePluginProjectFlags): DefaultAndroidGradlePluginProjectFlags {
    val flagMap: MutableMap<BooleanFlag, Boolean?> = mutableMapOf()
    BooleanFlag.values().forEach { flag ->
        // Use flag name to get value for better forward compatibility
        flags.getFlagValue(flag.name)?.let {
            flagMap[flag] = it
        }
    }
    return DefaultAndroidGradlePluginProjectFlags(flagMap)
  }

  fun copy(containers: Collection<SourceSetContainer>): Collection<DefaultSourceSetContainer> {
    return containers.map { copy(it) }
  }

  fun copy(container: SourceSetContainer): DefaultSourceSetContainer {
    return DefaultSourceSetContainer().apply {
      sourceProvider = copy(container.sourceProvider)
      testFixturesSourceProvider = copy(container.testFixturesSourceProvider)
      
      // Adapt to new map-based test providers
      androidTestSourceProvider = container.deviceTestSourceProviders["androidTest"]?.let { copy(it) }
      unitTestSourceProvider = container.hostTestSourceProviders["unitTest"]?.let { copy(it) }
    }
  }

  fun copy(provider: SourceProvider?): DefaultSourceProvider? {
    return provider?.let {
      DefaultSourceProvider().apply {
        name = it.name
        manifestFile = it.manifestFile
        javaDirectories = it.javaDirectories
        kotlinDirectories = it.kotlinDirectories
        resourcesDirectories = it.resourcesDirectories
        aidlDirectories = it.aidlDirectories
        renderscriptDirectories = it.renderscriptDirectories
        resDirectories = it.resDirectories
        assetsDirectories = it.assetsDirectories
        jniLibsDirectories = it.jniLibsDirectories
        shadersDirectories = it.shadersDirectories
        mlModelsDirectories = it.mlModelsDirectories
        customDirectories = copy(it.customDirectories)
        baselineProfileDirectories = it.baselineProfileDirectories
      }
    }
  }

  @JvmName("copyCustomSourceDirectories")
  private fun copy(
    directories: Collection<CustomSourceDirectory>?
  ): Collection<DefaultCustomSourceDirectory>? {
    return directories?.map { copy(it) }
  }

  private fun copy(it: CustomSourceDirectory): DefaultCustomSourceDirectory {
    return DefaultCustomSourceDirectory(it.directory, it.sourceTypeName)
  }

  fun copy(value: Library): DefaultLibrary {
    return DefaultLibrary().apply {
      this.key = value.key
      this.type = value.type
      this.projectInfo = copy(value.projectInfo)
      this.libraryInfo = copy(value.libraryInfo)
      this.artifact = value.artifact
      this.lintJar = value.lintJar
      this.androidLibraryData = copy(value.androidLibraryData)
      
      // Adapt to new srcJars list with fallback for older AGP versions
      // This prevents UnsupportedMethodException: Library.getSrcJars() on older Gradle daemons
      this.srcJars = try {
          value.srcJars
      } catch (e: Throwable) {
          // Fallback for older models where getSrcJars might not exist or throw AbstractMethodError/UnsupportedMethodException
          val fallbackList = mutableListOf<File>()
          try {
              @Suppress("DEPRECATION")
              value.srcJar?.let { fallbackList.add(it) }
          } catch (ignore: Throwable) {}
          
          try {
              @Suppress("DEPRECATION")
              value.samplesJar?.let { fallbackList.add(it) }
          } catch (ignore: Throwable) {}
          
          fallbackList
      }
      
      this.docJar = value.docJar
    }
  }

  private fun copy(info: ProjectInfo?): DefaultProjectInfo? {
    return info?.let {
      DefaultProjectInfo(
        it.attributes,
        it.buildType,
        it.capabilities,
        it.isTestFixtures,
        it.productFlavors,
        it.buildId,
        it.projectPath
      )
    }
  }

  private fun copy(info: LibraryInfo?): DefaultLibraryInfo? {
    return info?.let {
      DefaultLibraryInfo(
        it.attributes,
        it.buildType,
        it.capabilities,
        it.isTestFixtures,
        it.productFlavors,
        it.group,
        it.name,
        it.version
      )
    }
  }

  private fun copy(data: AndroidLibraryData?): DefaultAndroidLibraryData? {
    return data?.let {
      DefaultAndroidLibraryData(
        it.aidlFolder,
        it.assetsFolder,
        it.compileJarFiles,
        it.externalAnnotations,
        it.jniFolder,
        it.manifest,
        it.proguardRules,
        it.publicResources,
        it.renderscriptFolder,
        it.resFolder,
        it.resStaticLibrary,
        it.runtimeJarFiles,
        it.symbolFile
      )
    }
  }

  fun copy(artifact: ArtifactDependencies?): DefaultArtifactDependencies? =
    artifact?.let {
        DefaultArtifactDependencies().apply {
          this.compileDependencies = copy(it.compileDependencies)
          this.runtimeDependencies = it.runtimeDependencies?.let { deps -> copy(deps) }
          this.unresolvedDependencies = copy(it.unresolvedDependencies)
        }
    }

  private fun copy(dependencies: List<UnresolvedDependency>): List<DefaultUnresolvedDependency> {
    return dependencies.map { copy(it) }
  }

  private fun copy(it: UnresolvedDependency): DefaultUnresolvedDependency {
    return DefaultUnresolvedDependency(it.cause, it.name)
  }

  @JvmName("copyGraphItems")
  fun copy(graphs: List<GraphItem>): List<DefaultGraphItem> {
    if (graphs.isEmpty()) {
      return emptyList()
    }
    return graphs.map { copy(it) }
  }

  fun copy(graph: GraphItem): DefaultGraphItem =
    DefaultGraphItem().apply {
      this.key = graph.key
      this.dependencies = copy(graph.dependencies)
      this.requestedCoordinates = graph.requestedCoordinates
    }

  fun copy(issues: ProjectSyncIssues): DefaultProjectSyncIssues =
    DefaultProjectSyncIssues(copy(issues.syncIssues))

  @JvmName("copySyncIssue")
  fun copy(syncIssues: Collection<SyncIssue>): Collection<DefaultSyncIssue> {
    return syncIssues.map { copy(it) }
  }

  fun copy(issue: SyncIssue): DefaultSyncIssue =
    DefaultSyncIssue(
      issue.data, 
      issue.message,
      issue.multiLineMessage, 
      issue.severity, 
      issue.type
    )
}