package ru.zdevs.intellij.c.debug

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.dap.configurations.DAPRunConfiguration
import com.redhat.devtools.lsp4ij.dap.configurations.DAPSettingsEditor

class CRunConfigurationDefaultSettings(project: Project, val debuggerExecution: String) : DAPSettingsEditor(project) {

    private fun setDefaultConfigurationValue(runConfiguration: DAPRunConfiguration) {
        if (runConfiguration.command.isNullOrEmpty()) {
            runConfiguration.command = debuggerExecution
        }
    }

    override fun resetEditorFrom(runConfiguration: DAPRunConfiguration) {
        setDefaultConfigurationValue(runConfiguration)
        super.resetEditorFrom(runConfiguration)
    }

    override fun applyEditorTo(runConfiguration: DAPRunConfiguration) {
        setDefaultConfigurationValue(runConfiguration)
        super.applyEditorTo(runConfiguration)
    }
}