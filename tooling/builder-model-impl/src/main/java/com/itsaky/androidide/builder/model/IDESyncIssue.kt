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

import com.android.builder.model.v2.ide.SyncIssue

/**
 * Sync issue model for AndroidIDE.
 *
 * @author android_zero
 * Creation Time: 2025-11-09
 */
interface IDESyncIssue : SyncIssue {
  companion object {

    /**
     * Indicates that the Android Gradle Plugin that is being used by the project
     * is too new for AndroidIDE. Data is `projectAgpVersion:maxAgpVersion`.
     *
     * We use a value far from the official ones to avoid conflicts.
     */
    const val TYPE_AGP_VERSION_TOO_NEW = 101

    // Note: When adding new types, use values starting from 102.
    // The types that are defined in the official SyncIssue class have their values starting at 0.
  }
  
  
/**
 * Checks whether this [SyncIssue] should be ignored and not reported to the user.
 *
 * @return Whether the issue can be ignored.
 */
// fun SyncIssue.shouldBeIgnored() : Boolean {
  // if (this.type != SyncIssue.TYPE_UNSUPPORTED_PROJECT_OPTION_USE) {
    // return false
  // }

  // AndroidIDE sets android.aapt2FromMavenOverride in order to use a custom AAPT2 that is
  // compatible with Android
  // return AAPT2_FROM_MAVEN_OVERRIDE.propertyName == this.data
  
}