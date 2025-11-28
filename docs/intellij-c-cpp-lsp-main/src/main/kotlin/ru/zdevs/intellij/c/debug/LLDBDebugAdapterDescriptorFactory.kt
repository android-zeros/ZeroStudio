package ru.zdevs.intellij.c.debug

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterDescriptor
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterDescriptorFactory


class LLDBDebugAdapterDescriptorFactory : DebugAdapterDescriptorFactory() {

    override fun createDebugAdapterDescriptor(options: RunConfigurationOptions, environment: ExecutionEnvironment ): DebugAdapterDescriptor {
        return LLDBDebugAdapterDescriptor(options, environment, serverDefinition)
    }

    override fun getConfigurationEditor(project: Project): SettingsEditor<out RunConfiguration?> {
        return CRunConfigurationDefaultSettings(project, LLDBDebugAdapterDescriptor.DEBUGGER_EXEC_NAME[0])
    }
}