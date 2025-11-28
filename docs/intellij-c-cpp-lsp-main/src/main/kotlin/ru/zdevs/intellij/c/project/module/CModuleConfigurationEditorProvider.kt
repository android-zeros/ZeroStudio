package ru.zdevs.intellij.c.project.module

import com.intellij.openapi.module.ModuleConfigurationEditor
import com.intellij.openapi.roots.ui.configuration.CommonContentEntriesEditor
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState
import org.jetbrains.jps.model.java.JavaSourceRootType


class CModuleConfigurationEditorProvider : ModuleConfigurationEditorProvider {
    override fun createEditors(state: ModuleConfigurationState?): Array<ModuleConfigurationEditor> {
        if (state == null) {
            return ModuleConfigurationEditor.EMPTY
        }

        val module = state.currentRootModel.module
        if (module.moduleTypeName != CModuleType.ID) {
            return ModuleConfigurationEditor.EMPTY
        }

        return arrayOf(
            CommonContentEntriesEditor(
                module.name,
                state,
                JavaSourceRootType.SOURCE
            ), CModuleConfigurationEditor(module))
    }
}