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

import com.android.builder.model.v2.models.HostJarTestSuiteSource
import com.android.builder.model.v2.models.SourceType
import java.io.File
import java.io.Serializable

/**
 * @author android_zero
 * 
 * AGP 9.0 UPG: New implementation for the HostJarTestSuiteSource interface.
 * Represents the Java/Kotlin sources for a host-based test suite (e.g., unit tests).
 */
data class DefaultHostJarTestSuiteSource(
    override val name: String,
    override val java: Collection<File>,
    override val kotlin: Collection<File>
) : HostJarTestSuiteSource, Serializable {
    override val type: SourceType
        get() = SourceType.HOST_JAR
}