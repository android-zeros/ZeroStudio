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

package com.itsaky.androidide.tooling.api

import com.itsaky.androidide.builder.model.DefaultLibrary
import com.itsaky.androidide.builder.model.DefaultSourceSetContainer
import com.itsaky.androidide.tooling.api.models.AndroidVariantMetadata
import com.itsaky.androidide.tooling.api.models.BasicAndroidVariantMetadata
import com.itsaky.androidide.tooling.api.models.BasicTestSuiteMetadata
import com.itsaky.androidide.tooling.api.models.TestSuiteMetadata
import com.itsaky.androidide.tooling.api.models.params.StringParameter
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * Model for an Android project/module.
 *
 * @author android_zero
 * 
 * AGP 9.0 UPG: 
 * - Added `getTestSuites()` method to support querying extensible test suites.
 * - Added `getTestSuiteDetails()` for fetching detailed metadata of a specific test suite.
 */
@JsonSegment("android")
interface IAndroidProject : IModuleProject {

  /**
   * Get the variant that was configured/selected while building the model for this project.
   */
  @JsonRequest
  fun getConfiguredVariant(): CompletableFuture<String>

  /**
   * Get the metadata about all variants of this Android project.
   */
  @JsonRequest
  fun getVariants(): CompletableFuture<List<BasicAndroidVariantMetadata>>

  /**
   * Get the metadata about the variant with the given name.
   */
  @JsonRequest
  fun getVariant(param: StringParameter): CompletableFuture<AndroidVariantMetadata?>

  /**
   * Get the boot classpaths for this Android project.
   */
  @JsonRequest
  fun getBootClasspaths(): CompletableFuture<Collection<File>>

  /**
   * Get the map of libraries. Each entry is a unique key representing the library, and allowing
   * to match it with GraphItem instances.
   */
  @JsonRequest
  fun getLibraryMap(): CompletableFuture<Map<String, DefaultLibrary>>

  /**
   * Get the main source set container for this project.
   *
   * @return The main source set container or `null` if it is unavailable.
   */
  @JsonRequest
  fun getMainSourceSet(): CompletableFuture<DefaultSourceSetContainer?>

  /**
   * Get the lint check jars for this project.
   */
  @JsonRequest
  fun getLintCheckJars(): CompletableFuture<List<File>>

  /**
   * Get the basic metadata about all test suites of this Android project.
   * Available on AGP 9.0+
   */
  @JsonRequest
  fun getTestSuites(): CompletableFuture<List<BasicTestSuiteMetadata>>
  
  /**
   * Get the detailed metadata for a specific test suite by name.
   * Available on AGP 9.0+
   */
  @JsonRequest
  fun getTestSuiteDetails(param: StringParameter): CompletableFuture<TestSuiteMetadata?>

  @Suppress("unused")
  companion object {

    /**
     * The name of the Android project build variant that is used by default.
     */
    const val DEFAULT_VARIANT = "debug"

    const val ANDROID_NAMESPACE = "http://schemas.android.com/res/android"

    const val PROPERTY_BUILD_MODEL_V2_ONLY = "android.injected.build.model.v2"
    const val PROPERTY_AVOID_TASK_REGISTRATION = "android.injected.avoid.task.registration"
    const val PROPERTY_ANDROID_STUDIO_VERSION = "android.studio.version"
    const val PROPERTY_REFRESH_EXTERNAL_NATIVE_MODEL = "android.injected.refresh.external.native.model"
    const val PROPERTY_TEST_ONLY = "android.injected.testOnly"
    const val PROPERTY_BUILD_API = "android.injected.build.api"
    const val PROPERTY_BUILD_API_CODENAME = "android.injected.build.codename"
    const val PROPERTY_BUILD_ABI = "android.injected.build.abi"
    const val PROPERTY_BUILD_DENSITY = "android.injected.build.density"
    const val PROPERTY_INVOKED_FROM_IDE = "android.injected.invoked.from.ide"
    const val PROPERTY_BUILD_WITH_STABLE_IDS = "android.injected.enableStableIds"
    const val PROPERTY_SIGNING_STORE_FILE = "android.injected.signing.store.file"
    const val PROPERTY_SIGNING_STORE_PASSWORD = "android.injected.signing.store.password"
    const val PROPERTY_SIGNING_KEY_ALIAS = "android.injected.signing.key.alias"
    const val PROPERTY_SIGNING_KEY_PASSWORD = "android.injected.signing.key.password"
    const val PROPERTY_SIGNING_STORE_TYPE = "android.injected.signing.store.type"
    const val PROPERTY_SIGNING_V1_ENABLED = "android.injected.signing.v1-enabled"
    const val PROPERTY_SIGNING_V2_ENABLED = "android.injected.signing.v2-enabled"
    const val PROPERTY_DEPLOY_AS_INSTANT_APP = "android.injected.deploy.instant-app"
    const val PROPERTY_SIGNING_COLDSWAP_MODE = "android.injected.coldswap.mode"
    const val PROPERTY_APK_SELECT_CONFIG = "android.inject.apkselect.config"
    const val PROPERTY_EXTRACT_INSTANT_APK = "android.inject.bundle.extractinstant"
    const val PROPERTY_SUPPORTS_PRIVACY_SANDBOX = "android.inject.supports-privacy-sandbox"
    const val PROPERTY_VERSION_CODE = "android.injected.version.code"
    const val PROPERTY_VERSION_NAME = "android.injected.version.name"
    const val PROPERTY_APK_LOCATION = "android.injected.apk.location"
    const val PROPERTY_ATTRIBUTION_FILE_LOCATION = "android.injected.attribution.file.location"
    const val PROPERTY_INJECTED_DYNAMIC_MODULES_LIST = "android.injected.modules.install.list"
    const val FD_INTERMEDIATES = "intermediates"
    const val FD_LOGS = "logs"
    const val FD_OUTPUTS = "outputs"
    const val FD_GENERATED = "generated"
  }
}