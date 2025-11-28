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
package com.itsaky.androidide.builder.model

import com.android.builder.model.v2.ide.Library
import com.android.builder.model.v2.ide.LibraryType
import java.io.File
import java.io.Serializable

/** 
 * @author android_zero
 */
class DefaultLibrary : Library, Serializable {
  private val serialVersionUID = 1L
  override var androidLibraryData: DefaultAndroidLibraryData? = null
  override var artifact: File? = null
  override var docJar: File? = null
  override var key: String = ""
  override var libraryInfo: DefaultLibraryInfo? = null
  override var lintJar: File? = null
  override var projectInfo: DefaultProjectInfo? = null
  override var type: LibraryType = LibraryType.ANDROID_LIBRARY
  override var srcJars: List<File> = emptyList()

  @get:Deprecated("Use srcJars to get the source jar together with the sample source jar")
  override val srcJar: File?
    get() = srcJars.firstOrNull()

  @get:Deprecated("Sample source jar is now part of the source jars")
  override val samplesJar: File?
    get() = if (srcJars.size > 1) srcJars[1] else null

  /** Dependencies of this library. */
  val dependencies = mutableSetOf<String>()

  /**
   * Whether an attempt should be made to lookup this library's package name.
   *
   * FOR INTERNAL USE ONLY!
   */
  var lookupPackage: Boolean = true

  /**
   * The package name of this library. MUST NOT be accesed directly. Use
   * `DefaultLibrary.findPackageName()` method defined in the `:subprojects:tooling-api-models`
   * module.
   */
  var packageName: String = ""
}