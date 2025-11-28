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

import org.gradle.tooling.Failure
import java.io.Serializable

/**
 * @author android_zero
 * 
 * GRADLE 9.3 UPG: New models for the Gradle Problems API.
 * Contains core data classes for representing a structured problem.
 */

data class Problem(
    val definition: ProblemDefinition,
    val contextualLabel: ContextualLabel?,
    val details: Details?,
    val originLocations: List<Location>,
    val contextualLocations: List<Location>,
    val solutions: List<Solution>,
    val additionalData: AdditionalData,
    val failure: Failure?
) : Serializable

data class ProblemDefinition(
    val id: ProblemId,
    val severity: Severity,
    val documentationLink: DocumentationLink?
) : Serializable

data class ProblemId(
    val name: String,
    val displayName: String,
    val group: ProblemGroup
) : Serializable

data class ProblemGroup(
    val name: String,
    val displayName: String,
    val parent: ProblemGroup?
) : Serializable

enum class Severity(val level: Int) : Serializable {
    ADVICE(0),
    WARNING(1),
    ERROR(2);

    companion object {
        fun fromLevel(level: Int): Severity {
            return values().find { it.level == level } ?: WARNING
        }
    }
}

data class DocumentationLink(val url: String) : Serializable

data class Solution(val solution: String) : Serializable

data class Details(val details: String) : Serializable

data class ContextualLabel(val contextualLabel: String) : Serializable

interface AdditionalData : Serializable
data class DefaultAdditionalData(val data: Map<String, Any>) : AdditionalData
data class CustomAdditionalData(val data: Map<String, Any>, val proxiedData: Any?) : AdditionalData