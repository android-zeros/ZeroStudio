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

import com.android.builder.model.v2.ide.JUnitEngineInfo
import com.android.builder.model.v2.ide.TestSuiteArtifact
import com.android.builder.model.v2.ide.TestSuiteTarget
import com.android.builder.model.v2.ide.TestSuiteTestInfo
import com.android.builder.model.v2.ide.TestSuiteVariantTarget
import com.android.builder.model.v2.models.AssetsTestSuiteSource
import com.android.builder.model.v2.models.BasicTestSuite
import com.android.builder.model.v2.models.HostJarTestSuiteSource
import com.android.builder.model.v2.models.TestApkTestSuiteSource
import java.io.Serializable
import java.io.File

/**
 * @author android_zero
 * 
 * AGP 9.0 UPG: Consolidated all Test Suite related metadata classes into this single file.
 * This provides a unified and complete data model for representing extensible test suites
 * from AGP 9.0+, including factory methods for conversion from the builder-model.
 */

data class TestSuiteMetadata(
    val name: String,
    val assets: Collection<AssetsTestSuiteSourceMetadata>,
    val hostJars: Collection<HostJarTestSuiteSourceMetadata>,
    val testApks: Collection<TestApkTestSuiteSourceMetadata>,
    val targetsByVariant: Collection<TestSuiteVariantTargetMetadata>
) : Serializable {
    companion object {
        fun from(model: BasicTestSuite): TestSuiteMetadata {
            return TestSuiteMetadata(
                name = model.name,
                assets = model.assets.map { AssetsTestSuiteSourceMetadata.from(it) },
                hostJars = model.hostJars.map { HostJarTestSuiteSourceMetadata.from(it) },
                testApks = model.testApks.map { TestApkTestSuiteSourceMetadata.from(it) },
                targetsByVariant = model.targetsByVariant.map { TestSuiteVariantTargetMetadata.from(it) }
            )
        }
    }
}

data class AssetsTestSuiteSourceMetadata(
    val name: String,
    val directories: Collection<File>
) : Serializable {
    companion object {
        fun from(model: AssetsTestSuiteSource): AssetsTestSuiteSourceMetadata {
            return AssetsTestSuiteSourceMetadata(
                name = model.name,
                directories = model.directories
            )
        }
    }
}

data class HostJarTestSuiteSourceMetadata(
    val name: String,
    val javaDirectories: Collection<File>,
    val kotlinDirectories: Collection<File>
) : Serializable {
    companion object {
        fun from(model: HostJarTestSuiteSource): HostJarTestSuiteSourceMetadata {
            return HostJarTestSuiteSourceMetadata(
                name = model.name,
                javaDirectories = model.java,
                kotlinDirectories = model.kotlin
            )
        }
    }
}

data class TestApkTestSuiteSourceMetadata(
    val name: String
) : Serializable {
    companion object {
        fun from(model: TestApkTestSuiteSource): TestApkTestSuiteSourceMetadata {
            return TestApkTestSuiteSourceMetadata(
                name = model.name
            )
        }
    }
}

data class TestSuiteVariantTargetMetadata(
    val targetedVariant: String,
    val targets: Collection<TestSuiteTargetMetadata>
) : Serializable {
    companion object {
        fun from(model: TestSuiteVariantTarget): TestSuiteVariantTargetMetadata {
            return TestSuiteVariantTargetMetadata(
                targetedVariant = model.targetedVariant,
                targets = model.targets.map { TestSuiteTargetMetadata.from(it) }
            )
        }
    }
}

data class TestSuiteArtifactMetadata(
    val compileTaskName: String?,
    val assembleTaskName: String?,
    val classesFolders: Set<File>,
    val generatedSourceFolders: Collection<File>,
    val testInfo: TestSuiteTestInfoMetadata?
) : Serializable {
    companion object {
        fun from(model: TestSuiteArtifact): TestSuiteArtifactMetadata {
            return TestSuiteArtifactMetadata(
                compileTaskName = model.compileTaskName,
                assembleTaskName = model.assembleTaskName,
                classesFolders = model.classesFolders,
                generatedSourceFolders = model.generatedSourceFolders,
                testInfo = model.testInfo?.let { TestSuiteTestInfoMetadata.from(it) }
            )
        }
    }
}


data class TestSuiteTestInfoMetadata(
    val junitInfo: JUnitEngineInfoMetadata?,
    val targets: Map<String, TestSuiteTargetMetadata>
) : Serializable {
    companion object {
        fun from(model: TestSuiteTestInfo): TestSuiteTestInfoMetadata {
            return TestSuiteTestInfoMetadata(
                junitInfo = model.junitInfo?.let { JUnitEngineInfoMetadata.from(it) },
                targets = model.targets.mapValues { TestSuiteTargetMetadata.from(it.value) }
            )
        }
    }
}

data class TestSuiteTargetMetadata(
    val name: String,
    val testTaskName: String,
    val targetedDevices: Collection<String>
) : Serializable {
    companion object {
        fun from(model: TestSuiteTarget): TestSuiteTargetMetadata {
            return TestSuiteTargetMetadata(
                name = model.name,
                testTaskName = model.testTaskName,
                targetedDevices = model.targetedDevices
            )
        }
    }
}

data class JUnitEngineInfoMetadata(
    val includedEngines: Set<String>
) : Serializable {
    companion object {
        fun from(model: JUnitEngineInfo): JUnitEngineInfoMetadata {
            return JUnitEngineInfoMetadata(
                includedEngines = model.includedEngines
            )
        }
    }
}