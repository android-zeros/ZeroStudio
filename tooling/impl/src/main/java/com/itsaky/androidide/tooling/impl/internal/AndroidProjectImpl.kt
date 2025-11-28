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

import com.android.builder.model.v2.dsl.BuildType
import com.android.builder.model.v2.ide.AndroidArtifact
import com.android.builder.model.v2.ide.GraphItem
import com.android.builder.model.v2.ide.JavaArtifact
import com.android.builder.model.v2.ide.Library
import com.android.builder.model.v2.ide.ProjectType
import com.android.builder.model.v2.ide.Variant
import com.android.builder.model.v2.models.AndroidDsl
import com.android.builder.model.v2.models.AndroidProject
import com.android.builder.model.v2.models.BasicAndroidProject
import com.android.builder.model.v2.models.VariantDependencies
import com.android.builder.model.v2.models.Versions
import com.itsaky.androidide.builder.model.DefaultLibrary
import com.itsaky.androidide.builder.model.DefaultSourceSetContainer
import com.itsaky.androidide.builder.model.DefaultViewBindingOptions
import com.itsaky.androidide.tooling.api.IAndroidProject
import com.itsaky.androidide.tooling.api.models.AndroidArtifactMetadata
import com.itsaky.androidide.tooling.api.models.AndroidProjectMetadata
import com.itsaky.androidide.tooling.api.models.AndroidVariantMetadata
import com.itsaky.androidide.tooling.api.models.BasicAndroidVariantMetadata
import com.itsaky.androidide.tooling.api.models.BasicTestSuiteMetadata
import com.itsaky.androidide.tooling.api.models.ProjectMetadata
import com.itsaky.androidide.tooling.api.models.TestSuiteArtifactMetadata
import com.itsaky.androidide.tooling.api.models.TestSuiteMetadata
import com.itsaky.androidide.tooling.api.models.params.StringParameter
import com.itsaky.androidide.tooling.api.util.AndroidModulePropertyCopier
import com.itsaky.androidide.tooling.api.util.AndroidModulePropertyCopier.copy
import com.itsaky.androidide.tooling.api.models.PrivacySandboxSdkInfoMetadata
import com.itsaky.androidide.utils.AndroidPluginVersion
import org.gradle.tooling.model.GradleProject
import java.io.File
import java.io.Serializable
import java.util.concurrent.CompletableFuture

import com.itsaky.androidide.tooling.api.models.nativecpp.CppProjectMetadata

/**
 * @author android_zero
 * 
 * AGP 9.0 UPG: This class is heavily updated to align with the new builder-model interfaces.
 * - `getVariant` and related methods now correctly handle the new `deviceTestArtifacts`, `hostTestArtifacts`, and `testSuiteArtifacts` maps.
 * - Added comprehensive `toMetadata()` conversion functions for the new `TestSuiteArtifact` model and its components.
 * - Implemented `getTestSuiteDetails` to provide detailed information for a specific test suite.
 * - The legacy `applicationId` computation logic (`computeApplicationIdLegacy`) is refined for better accuracy with older AGP versions.
 * - `toMetadata()` conversion now includes new AGP 9.0 fields like `generatedAssetsFolders` and R8 mapping files.
 */
internal class AndroidProjectImpl(
  gradleProject: GradleProject,
  private val configuredVariant: String,
  private val basicAndroidProject: BasicAndroidProject,
  private val androidProject: AndroidProject,
  private val variantDependencies: VariantDependencies,
  private val versions: Versions,
  private val androidDsl: AndroidDsl,
  private val cppProjectMetadata: CppProjectMetadata?
) : GradleProjectImpl(gradleProject), IAndroidProject, Serializable {

  private val serialVersionUID = 1L

  constructor(base: AndroidProjectImpl, cppProjectMetadata: CppProjectMetadata?) : this(
      base.gradleProject,
      base.configuredVariant,
      base.basicAndroidProject,
      base.androidProject,
      base.variantDependencies,
      base.versions,
      base.androidDsl,
      cppProjectMetadata
  )

  override fun getConfiguredVariant(): CompletableFuture<String> {
    return CompletableFuture.completedFuture(this.configuredVariant)
  }

  override fun getVariants(): CompletableFuture<List<BasicAndroidVariantMetadata>> {
    return CompletableFuture.supplyAsync {
      androidProject.variants.map {
        BasicAndroidVariantMetadata(it.name, it.mainArtifact.toMetadata(it.name))
      }
    }
  }

  private fun AndroidArtifact.toMetadata(variantName: String): AndroidArtifactMetadata {
    return AndroidArtifactMetadata(
      name = variantName,
      applicationId = computeApplicationId(variantName),
      resGenTaskName = resGenTaskName,
      assembleTaskOutputListingFile = assembleTaskOutputListingFile,
      generatedResourceFolders = generatedResourceFolders,
      generatedSourceFolders = generatedSourceFolders,
      generatedAssetsFolders = generatedAssetsFolders,
      maxSdkVersion = maxSdkVersion,
      minSdkVersion = minSdkVersion.apiLevel,
      signingConfigName = signingConfigName,
      sourceGenTaskName = sourceGenTaskName,
      assembleTaskName = assembleTaskName,
      classJars = classesFolders.filter { it.name.endsWith(".jar") },
      compileTaskName = compileTaskName,
      targetSdkVersionOverride = targetSdkVersionOverride?.apiLevel ?: -1,
      mappingR8TextFile = mappingR8TextFile,
      mappingR8PartitionFile = mappingR8PartitionFile,
      privacySandboxSdkInfo = privacySandboxSdkInfo?.let { PrivacySandboxSdkInfoMetadata.from(it) },
      desugaredMethodsFiles = desugaredMethodsFiles
    )
  }

  override fun getVariant(param: StringParameter): CompletableFuture<AndroidVariantMetadata?> {
    return CompletableFuture.supplyAsync {
      androidProject.variants.find { it.name == param.value }?.toMetadata()
    }
  }

  private fun Variant.toMetadata(): AndroidVariantMetadata {
    val deviceTestArtifactsMetadata = deviceTestArtifacts.mapValues { it.value.toMetadata(name) }
    
    // Note: hostTestArtifacts are of type JavaArtifact and cannot be converted to AndroidArtifactMetadata directly.
    // This is a potential area for future enhancement if Java artifact metadata is needed.
    val testSuiteArtifactsMetadata = testSuiteArtifacts.mapValues { TestSuiteArtifactMetadata.from(it.value) }

    return AndroidVariantMetadata(
      name = name, 
      mainArtifact = mainArtifact.toMetadata(name),
      otherArtifacts = deviceTestArtifactsMetadata,
      testSuiteArtifacts = testSuiteArtifactsMetadata
    )
  }

  override fun getBootClasspaths(): CompletableFuture<Collection<File>> {
    return CompletableFuture.supplyAsync {
      basicAndroidProject.bootClasspath
    }
  }

  override fun getLibraryMap(): CompletableFuture<Map<String, DefaultLibrary>> {
    return CompletableFuture.supplyAsync {
      val seen = HashMap<String, DefaultLibrary>()
      val compileDependencies = variantDependencies.mainArtifact.compileDependencies
      val libraries = variantDependencies.libraries
      for (dependency in compileDependencies) {
        seen[dependency.key] ?: fillLibrary(dependency, libraries, seen)
      }
      seen
    }
  }

  private fun fillLibrary(item: GraphItem, libraries: Map<String, Library>,
    seen: HashMap<String, DefaultLibrary>): DefaultLibrary? {

    val lib = libraries[item.key] ?: return null
    val library = copy(lib)

    for (dependency in item.dependencies) {
      val dep = fillLibrary(dependency, libraries, seen)
      if (dep != null) {
          library.dependencies.add(dep.key)
      }
    }

    seen[item.key] = library

    return library
  }

  override fun getMainSourceSet(): CompletableFuture<DefaultSourceSetContainer?> {
    return CompletableFuture.supplyAsync {
      basicAndroidProject.mainSourceSet?.let(AndroidModulePropertyCopier::copy)
    }
  }

  override fun getLintCheckJars(): CompletableFuture<List<File>> {
    return CompletableFuture.supplyAsync { androidProject.lintChecksJars }
  }
  
  override fun getTestSuites(): CompletableFuture<List<BasicTestSuiteMetadata>> {
    return CompletableFuture.supplyAsync {
        try {
            basicAndroidProject.testSuites.map {
                BasicTestSuiteMetadata(it.name)
            }
        } catch (e: UnsupportedOperationException) {
            // Older AGP versions do not have `testSuites`, return empty list for compatibility
            emptyList()
        }
    }
  }
  
  override fun getTestSuiteDetails(param: StringParameter): CompletableFuture<TestSuiteMetadata?> {
      return CompletableFuture.supplyAsync {
          try {
              basicAndroidProject.testSuites
                  .find { it.name == param.value }
                  ?.let { TestSuiteMetadata.from(it) }
          } catch (e: UnsupportedOperationException) {
              // Older AGP versions do not have `testSuites`
              null
          }
      }
  }

  private fun getClassesJar(): File {
    // TODO(itsaky): this should handle product flavors as well
    return File(gradleProject.buildDirectory,
      "${IAndroidProject.FD_INTERMEDIATES}/compile_library_classes_jar/$configuredVariant/classes.jar")
  }

  override fun getClasspaths(): CompletableFuture<List<File>> {
    return CompletableFuture.supplyAsync {
      mutableListOf<File>().apply {
        add(getClassesJar())
        getVariant(StringParameter(configuredVariant)).get()?.mainArtifact?.classJars?.let(
          this::addAll)
      }
    }
  }

  override fun getMetadata(): CompletableFuture<ProjectMetadata> {
    return CompletableFuture.supplyAsync {
      val gradleMetadata = super.getMetadata().get()

      val viewBindingOptions = androidProject.viewBindingOptions?.let(
        AndroidModulePropertyCopier::copy) ?: DefaultViewBindingOptions()
      
      val javaOptions = androidProject.javaCompileOptions?.let { copy(it) }

      return@supplyAsync AndroidProjectMetadata(gradleMetadata,
        basicAndroidProject.projectType, copy(androidProject.flags),
        javaOptions, viewBindingOptions, androidProject.resourcePrefix,
        androidProject.namespace, androidProject.androidTestNamespace,
        androidProject.testFixturesNamespace, getClassesJar())
    }
  }
  
  private fun AndroidArtifact.computeApplicationId(variantName: String): String? {
    // AGP 7.4+ is expected to provide the applicationId directly.
    // For older versions, fallback to legacy computation.
    val minAgpForAppId = AndroidPluginVersion(7, 4, 0)
    val agpVersion = AndroidPluginVersion.parse(versions.agp)
    return if (agpVersion >= minAgpForAppId) {
      applicationId
    } else {
      computeApplicationIdLegacy(variantName)
    }
  }

  private fun computeApplicationIdLegacy(variantName: String): String {
    val basicVariant = basicAndroidProject.variants.firstOrNull { it.name == variantName }
    val buildType = basicVariant?.buildType?.let { buildTypeName ->
      androidDsl.buildTypes.find { it.name == buildTypeName }
    } ?: throw IllegalStateException("Build type for variant '$variantName' not found.")

    val appIdFromFlavor = if (basicAndroidProject.projectType == ProjectType.APPLICATION) {
      val flavorNames = basicVariant.productFlavors
      val flavors = androidDsl.productFlavors.filter { flavorNames.contains(it.name) }
      
      val mergedFlavor = flavors.reversed().firstOrNull { it.applicationId != null }
      mergedFlavor?.applicationId ?: androidDsl.defaultConfig.applicationId
    } else {
      androidDsl.defaultConfig.applicationId
    }

    return if (appIdFromFlavor == null) {
      "${androidProject.namespace}${computeApplicationIdSuffix(variantName, buildType)}"
    } else {
      "$appIdFromFlavor${computeApplicationIdSuffix(variantName, buildType)}"
    }
  }

  private fun computeApplicationIdSuffix(variantName: String, buildType: BuildType): String {
    val basicVariant = basicAndroidProject.variants.firstOrNull { it.name == variantName }
        ?: return ""
    
    val suffixes = mutableListOf<String>()
    androidDsl.defaultConfig.applicationIdSuffix?.let {
      suffixes.add(it)
    }

    if (basicAndroidProject.projectType == ProjectType.APPLICATION) {
      val flavorNames = basicVariant.productFlavors
      val flavors = androidDsl.productFlavors.filter { flavorNames.contains(it.name) }
      
      flavors.forEach { flavor ->
        flavor.applicationIdSuffix?.let { suffixes.add(it) }
      }
      
      buildType.applicationIdSuffix?.also {
        suffixes.add(it)
      }
    }

    val nonEmptySuffixes = suffixes.filter { it.isNotEmpty() }
    return if (nonEmptySuffixes.isNotEmpty()) {
      ".${nonEmptySuffixes.joinToString(separator = ".", transform = { it.removePrefix(".") })}"
    } else {
      ""
    }
  }
  
  override fun getCppMetadata(): CompletableFuture<CppProjectMetadata?> {
      return CompletableFuture.completedFuture(cppProjectMetadata)
  }
  


}