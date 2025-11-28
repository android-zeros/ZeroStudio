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

import com.itsaky.androidide.tooling.events.OperationDescriptor
import com.itsaky.androidide.tooling.events.ProgressEvent

/**
 * @author android_zero
 * 
 * GRADLE 9.3 UPG: New models for the Gradle Problems API.
 * Defines the event types for reporting problems.
 */

sealed class ProblemEvent(
    final override val eventTime: Long,
    final override val displayName: String,
    final override val descriptor: OperationDescriptor
) : ProgressEvent()

class SingleProblemEvent(
    eventTime: Long,
    displayName: String,
    descriptor: OperationDescriptor,
    val problem: Problem
) : ProblemEvent(eventTime, displayName, descriptor)

// Stubs for future implementation if needed
class ProblemAggregationEvent(
    eventTime: Long,
    displayName: String,
    descriptor: OperationDescriptor
    // val problemAggregation: ProblemAggregation
) : ProblemEvent(eventTime, displayName, descriptor)

class ProblemSummariesEvent(
    eventTime: Long,
    displayName: String,
    descriptor: OperationDescriptor
    // val problemSummaries: List<ProblemSummary>
) : ProblemEvent(eventTime, displayName, descriptor)