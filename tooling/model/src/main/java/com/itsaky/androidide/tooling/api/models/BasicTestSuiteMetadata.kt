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

import java.io.Serializable

/**
 * @author android_zero
 *
 * AGP 9.0 UPG: New data transfer object for BasicTestSuite.
 * Provides a lightweight representation of a test suite for dynamic discovery.
 */
data class BasicTestSuiteMetadata(
    val name: String
    // We can add more basic info here in the future if needed,
    // e.g., the type of test suite (host vs device).
) : Serializable