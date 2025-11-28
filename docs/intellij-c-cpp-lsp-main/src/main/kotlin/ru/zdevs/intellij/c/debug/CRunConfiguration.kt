package ru.zdevs.intellij.c.debug

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.dap.configurations.DAPRunConfiguration

class CRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    DAPRunConfiguration(project, factory, name),
    RunProfileWithCompileBeforeLaunchOption {

    fun getRunConfigurationOptions() : CRunConfigurationOptions {
        return options as CRunConfigurationOptions
    }

    override fun getConfigurationEditor() : SettingsEditor<RunConfiguration> {
        return CRunConfigurationEditor() as SettingsEditor<RunConfiguration>
    }

    override fun copyTo(configuration: DAPRunConfiguration) {
        super.copyTo(configuration)

        if (configuration is CRunConfiguration) {
            configuration.getRunConfigurationOptions().stopAtMain = getRunConfigurationOptions().stopAtMain
        }
    }
}
