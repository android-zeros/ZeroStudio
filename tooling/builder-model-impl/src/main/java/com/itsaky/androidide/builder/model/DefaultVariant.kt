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

import com.android.builder.model.v2.ide.AndroidArtifact
import com.android.builder.model.v2.ide.JavaArtifact
import com.android.builder.model.v2.ide.TestSuiteArtifact
import com.android.builder.model.v2.ide.Variant
import java.io.File
import java.io.Serializable

/**
 * @author android_zero
 *
 * AGP 9.0 UPG: Replaced specific test artifacts (androidTest, unitTest)
 * with generalized maps (deviceTestArtifacts, hostTestArtifacts, testSuiteArtifacts)
 * to support extensible test types.
 */
class DefaultVariant : Variant, Serializable {

    private val serialVersionUID = 1L

    override var name: String = ""
    override var displayName: String = ""
    override var mainArtifact: DefaultAndroidArtifact = DefaultAndroidArtifact()

    @get:Deprecated("Contained in deviceTestArtifacts",
        replaceWith = ReplaceWith("deviceTestArtifacts[\"androidTest\"]")
    )
    override val androidTestArtifact: AndroidArtifact?
        get() = deviceTestArtifacts["androidTest"]

    @get:Deprecated("Contained in hostTestArtifacts",
        replaceWith = ReplaceWith("hostTestArtifacts[\"unitTest\"]")
    )
    override val unitTestArtifact: JavaArtifact?
        get() = hostTestArtifacts["unitTest"]

    override var testFixturesArtifact: DefaultAndroidArtifact? = null
    override var testedTargetVariant: DefaultTestedTargetVariant? = null
    override var isInstantAppCompatible: Boolean = false
    override var desugaredMethods: List<File> = emptyList()

    // New properties introduced in AGP 9.0+ models
    override val deviceTestArtifacts: Map<String, AndroidArtifact> = emptyMap()
    override val hostTestArtifacts: Map<String, JavaArtifact> = emptyMap()
    override val testSuiteArtifacts: Map<String, TestSuiteArtifact> = emptyMap()
    override val runTestInSeparateProcess: Boolean = false
    override val experimentalProperties: Map<String, String> = emptyMap()
}