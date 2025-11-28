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

package com.itsaky.androidide.tooling.api.models

/**
 * Metadata about a variant in an Android project.
 *
 * @author android_zero
 * 
 * AGP 9.0 UPG:
 * - Added `otherArtifacts` map to support multiple test artifacts.
 * - Added `testSuiteArtifacts` to support the new extensible test suite model from AGP 9.0+.
 * Original author: Akash Yadav
 */
open class AndroidVariantMetadata(
  name: String,
  mainArtifact: AndroidArtifactMetadata,

  /**
   * Artifacts of this variant other than the [mainArtifact].
   * This typically includes standard test artifacts, such as `androidTest`, keyed by their name.
   */
  val otherArtifacts: Map<String, AndroidArtifactMetadata>,
  
  /**
   * Artifacts for the extensible test suites of this variant, keyed by the test suite's name.
   * This is part of the new Test Suite API in AGP 9.0+.
   */
  val testSuiteArtifacts: Map<String, TestSuiteArtifactMetadata>

) : BasicAndroidVariantMetadata(name, mainArtifact)