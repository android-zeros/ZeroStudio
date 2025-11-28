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

import com.android.builder.model.v2.ide.PrivacySandboxSdkInfo
import java.io.File
import java.io.Serializable

/**
 * @author android_zero
 * 
 * AGP 9.0 UPG: New data transfer object for PrivacySandboxSdkInfo.
 * This class is used to transfer privacy sandbox build details across the Tooling API.
 */
data class PrivacySandboxSdkInfoMetadata(
    val task: String,
    val outputListingFile: File,
    val additionalApkSplitTask: String,
    val additionalApkSplitFile: File,
    val taskLegacy: String,
    val outputListingLegacyFile: File
) : Serializable {
    companion object {
        fun from(model: PrivacySandboxSdkInfo): PrivacySandboxSdkInfoMetadata {
            return PrivacySandboxSdkInfoMetadata(
                task = model.task,
                outputListingFile = model.outputListingFile,
                additionalApkSplitTask = model.additionalApkSplitTask,
                additionalApkSplitFile = model.additionalApkSplitFile,
                taskLegacy = model.taskLegacy,
                outputListingLegacyFile = model.outputListingLegacyFile
            )
        }
    }
}