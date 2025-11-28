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

import com.itsaky.androidide.tooling.api.IModuleProject
import com.itsaky.androidide.tooling.api.messages.InitializeProjectParams
import com.itsaky.androidide.tooling.impl.internal.AndroidProjectImpl
import com.itsaky.androidide.tooling.impl.internal.JavaProjectImpl
import com.itsaky.androidide.tooling.impl.internal.toMetadata
import org.gradle.tooling.model.cpp.CppProject

/**
 * Builds models for module projects (either Android app/library or Java library projects).
 *
 * @author Akash Yadav
 *@author android_zero
 */
class ModuleProjectModelBuilder(initializationParams: InitializeProjectParams) :
  AbstractModelBuilder<ModuleProjectModelBuilderParams, IModuleProject>(initializationParams) {

  override fun build(param: ModuleProjectModelBuilderParams): IModuleProject {
    val versions = getAndroidVersions(param.module, param.controller)

    val cppProjectModel = param.controller.findModel(param.module, CppProject::class.java)
    val cppMetadata = cppProjectModel?.toMetadata()

    return if (versions != null) {
      checkAgpVersion(versions, param.syncIssueReporter)
      val baseProject = AndroidProjectModelBuilder(initializationParams)
        .build(AndroidProjectModelBuilderParams(
          param.controller,
          param.module,
          versions,
          param.syncIssueReporter
        )) as AndroidProjectImpl
      
      AndroidProjectImpl(baseProject, cppMetadata)

    } else {
      val baseProject = JavaProjectModelBuilder(initializationParams).build(
        JavaProjectModelBuilderParams(param)
      ) as JavaProjectImpl
      
      JavaProjectImpl(baseProject, cppMetadata)
    }
  }
}