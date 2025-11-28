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

import com.android.builder.model.v2.ide.TestSuiteTarget
import com.android.builder.model.v2.ide.TestSuiteVariantTarget
import java.io.Serializable

/**
 * @author android_zero
 * 
 * AGP 9.0 UPG: New implementation for the TestSuiteVariantTarget interface.
 * Links a test suite to a specific build variant and its test targets.
 */
data class DefaultTestSuiteVariantTarget(
    override val targetedVariant: String,
    override val targets: Collection<TestSuiteTarget>
) : TestSuiteVariantTarget, Serializable