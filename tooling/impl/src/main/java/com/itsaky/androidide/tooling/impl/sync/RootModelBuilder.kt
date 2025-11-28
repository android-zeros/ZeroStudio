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

package com.itsaky.androidide.tooling.impl.sync

import com.android.builder.model.AndroidProject
import com.android.builder.model.AndroidProject.PROPERTY_AVOID_TASK_REGISTRATION
import com.android.builder.model.PROPERTY_BUILD_MODEL_V2_ONLY
import com.android.builder.model.PROPERTY_INVOKED_FROM_IDE
import com.android.builder.model.v2.ide.SyncIssue
import com.itsaky.androidide.builder.model.DefaultProjectSyncIssues
import com.itsaky.androidide.builder.model.DefaultSyncIssue
import com.itsaky.androidide.tooling.api.IProject
import com.itsaky.androidide.tooling.api.messages.InitializeProjectParams
import com.itsaky.androidide.tooling.api.util.AndroidModulePropertyCopier
import com.itsaky.androidide.tooling.impl.Main
import com.itsaky.androidide.tooling.impl.Main.finalizeLauncher
import com.itsaky.androidide.tooling.impl.internal.ProjectImpl
import org.gradle.tooling.ConfigurableLauncher
import org.gradle.tooling.FetchModelResult
import org.gradle.tooling.internal.protocol.resiliency.InternalFetchAwareBuildController
import org.gradle.tooling.model.idea.IdeaProject
import org.slf4j.LoggerFactory
import java.io.Serializable

/**
 * Utility class to build the project models.
 *
 * @author android_zero
 * 
 * GRADLE 9.3 UPG: 
 * - Refactored to use the resilient `fetch()` API for newer Gradle versions.
 * - Added a fallback to the traditional `getModel()` API for older Gradle versions.
 * - FIX: Resolved `Unresolved reference` errors by importing constants directly from `SyncIssue` 
 *   and explicitly providing type arguments for the `fetch` method.
 * Original author: Akash Yadav
 */
class RootModelBuilder(initializationParams: InitializeProjectParams) :
  AbstractModelBuilder<RootProjectModelBuilderParams, IProject>(initializationParams),
  Serializable {

  private val serialVersionUID = 1L

  override fun build(param: RootProjectModelBuilderParams): IProject {

    val (projectConnection, cancellationToken) = param

    val initializationParams = this.initializationParams

    val executor = projectConnection.action { controller ->
      
      val syncIssues = hashSetOf<DefaultSyncIssue>()
      val syncIssueReporter = ISyncIssueReporter { issue ->
        val defaultSyncIssue = issue as? DefaultSyncIssue ?: AndroidModulePropertyCopier.copy(issue)
        syncIssues.add(defaultSyncIssue)
      }

      val ideaProject: IdeaProject

      if (controller is InternalFetchAwareBuildController) {
        // --- New path for Gradle 9.3+ with fetch API ---
        fun <T> processFetchResult(result: FetchModelResult<T>, modelName: String, modulePath: String): T? {
            if (result.failures.isNotEmpty()) {
                val failure = result.failures.first()
                val issue = DefaultSyncIssue(
                    data = modulePath,
                    message = "Failed to fetch model '$modelName' for project '$modulePath'. Reason: ${failure.message}",
                    multiLineMessage = failure.causes.map { it.message },
                    severity = SyncIssue.SEVERITY_ERROR,
                    type = SyncIssue.TYPE_GENERIC
                )
                syncIssueReporter.report(issue)
                return null
            }
            return result.model
        }
        
        // Explicitly specify type arguments for fetch
        val ideaProjectResult = controller.fetch(null, IdeaProject::class.java, Void::class.java, null)
        ideaProject = processFetchResult(ideaProjectResult, "IdeaProject", ":")
            ?: throw ModelBuilderException("Unable to fetch IdeaProject model for the root project.")

      } else {
        // --- Fallback path for older Gradle versions ---
        ideaProject = controller.getModelAndLog(IdeaProject::class.java)
      }

      val ideaModules = ideaProject.modules
      val modulePaths = mapOf(*ideaModules.map { it.name to it.gradleProject.path }.toTypedArray())
      val rootModule = ideaModules.find { it.gradleProject.parent == null }
        ?: throw ModelBuilderException("Unable to find root project")

      val rootProjectVersions = getAndroidVersions(rootModule, controller)

      val rootProject = if (rootProjectVersions != null) {
        checkAgpVersion(rootProjectVersions, syncIssueReporter)
        
        val builderParams = AndroidProjectModelBuilderParams(
            controller,
            rootModule,
            rootProjectVersions,
            syncIssueReporter
        )
        AndroidProjectModelBuilder(initializationParams).build(builderParams)
      } else {
        GradleProjectModelBuilder(initializationParams).build(rootModule.gradleProject)
      }

      val projects = ideaModules.mapNotNull { ideaModule ->
        try {
            ModuleProjectModelBuilder(initializationParams).build(
              ModuleProjectModelBuilderParams(
                controller,
                ideaProject,
                ideaModule,
                modulePaths,
                syncIssueReporter
              ))
        } catch (e: Exception) {
            val issue = DefaultSyncIssue(
                data = ideaModule.gradleProject.path,
                message = "Failed to build model for project '${ideaModule.gradleProject.path}'. Reason: ${e.message}",
                multiLineMessage = null,
                severity = SyncIssue.SEVERITY_ERROR,
                type = SyncIssue.TYPE_GENERIC
            )
            syncIssueReporter.report(issue)
            null
        }
      }

      return@action ProjectImpl(
        rootProject,
        rootModule.gradleProject.path,
        projects,
        DefaultProjectSyncIssues(syncIssues)
      )
    }

    finalizeLauncher(executor)
    applyAndroidModelBuilderProps(executor)

    if (cancellationToken != null) {
      executor.withCancellationToken(cancellationToken)
    }

    val logger = LoggerFactory.getLogger("RootModelBuilder")
    logger.warn("Starting build. See build output for more details...")

    if (Main.client != null) {
      Main.client.logOutput("Starting build...")
    }

    return executor.run().also {
      logger.debug("Build action executed. Result: {}", it)
    }
  }

  private fun applyAndroidModelBuilderProps(
    launcher: ConfigurableLauncher<*>) {
    launcher.addProperty(PROPERTY_BUILD_MODEL_V2_ONLY, true)
    launcher.addProperty(PROPERTY_INVOKED_FROM_IDE, true)
    launcher.addProperty(PROPERTY_AVOID_TASK_REGISTRATION, true)
  }

  private fun ConfigurableLauncher<*>.addProperty(property: String, value: Any) {
    addArguments(String.format("-P%s=%s", property, value))
  }
}