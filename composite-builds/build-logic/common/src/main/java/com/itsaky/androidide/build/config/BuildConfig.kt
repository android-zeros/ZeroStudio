package com.itsaky.androidide.build.config/*
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

import org.gradle.api.JavaVersion

/**
 * Build configuration for the IDE.
 *
 *import com.itsaky.androidide.build.config.BuildConfig
 *
 * @author Akash Yadav
 */
object BuildConfig {

  /** AndroidIDE's package name. */
  const val packageName = "com.itsaky.androidide"

  /** The compile SDK version. */
  const val compileSdk = 34
  /* Android build Platform tools*/
  const val BuildCompileSdk = "34.0.0"

  /** The minimum SDK version. */
  const val minSdk = 26

  /** The target SDK version. */
  const val targetSdk = 28

  const val ndkVersion = "27.1.12297006" //26.1.10909125

  /** The source and target Java compatibility. */
  val javaVersion = JavaVersion.VERSION_11
  val javaVersion13 = JavaVersion.VERSION_13
  val javaVersion17 = JavaVersion.VERSION_17
  val javaVersion21 = JavaVersion.VERSION_21
  val javaVersion22 = JavaVersion.VERSION_22
  val javaVersion23 = JavaVersion.VERSION_23
  val javaVersion24 = JavaVersion.VERSION_24
  val javaVersion25 = JavaVersion.VERSION_25
  
  /** ABI: arm64-v8a (64-bit) */
  const val ABI_ARM64_V8A = "arm64-v8a"
  /** ABI: armeabi-v7a (32-bit) */
  const val ABI_ARMEABI_V7A = "armeabi-v7a"
  /** ABI: x86 (32-bit) */
  const val ABI_X86 = "x86"
  /** ABI: x86_64 (64-bit) */
  const val ABI_X86_64 = "x86_64"
  
    /*SetupAapt2Task.class Android build Platform appt2 tools*/
  const val SetupAapt2Task = "34.0.4"
  /* SetupAapt2Task.class appt2 download url*/
  const val AAPT2_DOWNLOAD_URL = "https://github.com/AndroidIDEOfficial/platform-tools/releases/download/v%1\$s/aapt2-%2\$s"
  /** SetupAapt2Task.class appt2 arm64-v8a check sha256 */
  const val ABI_ARM64_V8A_AAPT2_CHECKSUMS = "be2cea61814678f7a9e61bf818a6666e6097a7d67d6c19498a4d7aa690bc4151"
   /** SetupAapt2Task.class appt2 armeabi-v7a check sha256 */
   const val ABI_ARMEABI_V7A_AAPT2_CHECKSUMS = "ba3413c680933dffd3c3d35da8d450c474ff5ccab95c4b9db28841c53b7a3cdf"
     /** SetupAapt2Task.class appt2 x86_64 check sha256 */
   const val ABI_X86_64_AAPT2_CHECKSUMS = "4861171c1efcffe41f4466937e6a392b243ffb014813b4e60f0b77bb46ab254d"
    /** SetupAapt2Task.class appt2 x86 check sha256 */
    // const val ABI_X86_AAPT2_CHECKSUMS = ""
    
    /*com.itsaky.androidide.build.config.config.kt
   * The minimum Android Gradle Plugin version which is supported by AndroidIDE. */
   const val AGP_VERSION_MINIMUM = "7.2.0"
   
   /**com.itsaky.androidide.plugins.tasks.GradleWrapperGeneratorTask*/
  /** The gradle/wrapper/gradle-wrapper.properties used to create the Android project template*/
    private const val GRADLE_wrapper_VERSION = "7.4.2"
   /*git ci
   *com.itsaky.androidide.build.config.ProjectConfig*/
  const val REPO_HOST = "github.com"
  const val REPO_OWNER = "AndroidIDEOfficial"
  const val REPO_NAME = "AndroidIDE"
  const val REPO_URL = "https://$REPO_HOST/$REPO_OWNER/$REPO_NAME"
  const val SCM_GIT = "scm:git:git://$REPO_HOST/$REPO_OWNER/$REPO_NAME.git"
  const val SCM_SSH =  "scm:git:ssh://git@$REPO_HOST/$REPO_OWNER/$REPO_NAME.git"
  const val PROJECT_SITE = "https://m.androidide.com"
  const val GIT_Branch_Name = "ZeroStudio-devs"
  
  /* The Sonatype snapshots repository.*/
  const val SONATYPE_SNAPSHOTS_REPO = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
  /* The Sonatype release repository.*/
  const val SONATYPE_PUBLIC_REPO = "https://s01.oss.sonatype.org/content/groups/public/"
  
  /** com.itsaky.androidide.plugins.tasks.internal.WrapperDefaults*/
  const val SCRIPT_PATH = "gradlew";
  const val JAR_FILE_PATH = "gradle/wrapper/gradle-wrapper.jar";
  
  /**com.itsaky.androidide.plugins.TerminalBootstrapPackagesPlugin*/
  /*arm32 abi*/
  const val ABI_ARM32 = "arm"
  /*arm64 abi*/
  const val ABI_ARMEABI_AARCH64 = "aarch64"
  /** terminal bootstrap download file arm64 abi check sha256 hash*/
  const val BOOTSTRAP_PACKAGES_ABI_ARMEABI_AARCH64 =  "d10fa952769b07b3d0babf6e155aacf58e5d441bb0ed87d4f9e8e54b73575180"
    /** terminal bootstrap download file arm32 abi check sha256 hash*/
  const val BOOTSTRAP_PACKAGES_ABI_ARM32 = "0c87c46b5a3ca04035f831d6271b9ef241ce55bc26f6ec1539017901853e3c9e"
    /** terminal bootstrap download file x86_64 abi check sha256 hash*/
  const val BOOTSTRAP_PACKAGES_ABI_X86_64 = "00dcbe6c8f8bf09a996128789cf54e2586bb07a05d4cb2f217c18af592e466b0"
  /** The terminal bootstrap file download URL*/
  const val PACKAGES_DOWNLOAD_URL = "https://github.com/msmt2017/termux-packages-zero/releases/download/bootstrap-%1\$s/bootstrap-%2\$s.zip"
  /** terminal bootstrap package files version*/
  const val BOOTSTRAP_PACKAGES_VERSION = "03.03.2025"
  
  
}
