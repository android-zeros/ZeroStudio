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

import com.android.builder.model.v2.ide.ArtifactDependencies
import com.android.builder.model.v2.models.TestSuiteDependencies
import com.android.builder.model.v2.models.VariantDependencies
import java.io.Serializable

/**
 * @author android_zero
 *
 * AGP 9.0 UPG: Replaced specific test artifact dependencies with generalized maps
 * (deviceTestArtifacts, hostTestArtifacts, testSuiteArtifacts) to align with Variant changes.
 */
class DefaultVariantDependencies : VariantDependencies, Serializable {

    private val serialVersionUID = 1L

    override var name: String = ""
    override var mainArtifact: DefaultArtifactDependencies = DefaultArtifactDependencies()

    @get:Deprecated("Contained in deviceTestArtifacts",
        replaceWith = ReplaceWith("deviceTestArtifacts[\"androidTest\"]")
    )
    override val androidTestArtifact: ArtifactDependencies?
        get() = deviceTestArtifacts["androidTest"]


    @get:Deprecated("Contained in hostTestArtifacts",
        replaceWith = ReplaceWith("hostTestArtifacts[\"unitTest\"]")
    )
    override val unitTestArtifact: ArtifactDependencies?
        get() = hostTestArtifacts["unitTest"]

    override var testFixturesArtifact: DefaultArtifactDependencies? = null
    override var libraries: Map<String, DefaultLibrary> = emptyMap()

    // New properties introduced in AGP 9.0+ models
    override val deviceTestArtifacts: Map<String, ArtifactDependencies> = emptyMap()
    override val hostTestArtifacts: Map<String, ArtifactDependencies> = emptyMap()
    override val testSuiteArtifacts: Map<String, TestSuiteDependencies> = emptyMap()
}