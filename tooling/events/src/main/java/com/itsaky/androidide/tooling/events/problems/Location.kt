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
package com.itsaky.androidide.tooling.events.problems

import java.io.Serializable

/**
 * @author android_zero
 * 
 * GRADLE 9.3 UPG: New models for the Gradle Problems API.
 * Represents the location of a problem.
 */
sealed interface Location : Serializable

data class FileLocation(val path: String) : Location

data class LineInFileLocation(
    val path: String,
    val line: Int,
    val column: Int,
    val length: Int
) : Location

data class OffsetInFileLocation(
    val path: String,
    val offset: Int,
    val length: Int
) : Location

data class TaskPathLocation(val buildTreePath: String) : Location

data class PluginIdLocation(val pluginId: String) : Location