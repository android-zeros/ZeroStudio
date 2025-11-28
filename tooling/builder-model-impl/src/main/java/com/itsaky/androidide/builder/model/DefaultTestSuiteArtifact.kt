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

import com.android.builder.model.v2.ide.BytecodeTransformation
import com.android.builder.model.v2.ide.TestSuiteArtifact
import com.android.builder.model.v2.ide.TestSuiteTestInfo
import java.io.File
import java.io.Serializable

/**
 * @author android_zero
 * 
 * AGP 9.0 UPG: New implementation for the TestSuiteArtifact interface introduced in AGP 9.0,
 * used to model extensible test suites.
 */
class DefaultTestSuiteArtifact : TestSuiteArtifact, Serializable {

    private val serialVersionUID = 1L

    override lateinit var testInfo: TestSuiteTestInfo
    override var compileTaskName: String? = null
    override var assembleTaskName: String? = null
    override var classesFolders: Set<File> = emptySet()
    override var ideSetupTaskNames: Set<String> = emptySet()
    override var generatedSourceFolders: Collection<File> = emptyList()

    @get:Deprecated("Removed: Previously returned model sync files which were never used.")
    override val modelSyncFiles: Collection<Void> = emptyList()

    override val generatedClassPaths: Map<String, File> = emptyMap()
    override val bytecodeTransformations: Collection<BytecodeTransformation> = emptyList()
}