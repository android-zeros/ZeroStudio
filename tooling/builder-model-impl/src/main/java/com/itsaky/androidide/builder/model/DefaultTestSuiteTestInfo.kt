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

import com.android.builder.model.v2.ide.TestSuiteTestInfo
import java.io.Serializable

/**
 * @author android_zero
 * 
 * AGP 9.0 UPG: New implementation for the TestSuiteTestInfo interface.
 * This class holds detailed information about a test suite's configuration.
 * FIXED: `junitInfo` is now non-nullable to match the interface.
 */
data class DefaultTestSuiteTestInfo(
    override val junitInfo: DefaultJUnitEngineInfo,
    override val targets: Map<String, DefaultTestSuiteTarget>
) : TestSuiteTestInfo, Serializable