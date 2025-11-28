package ru.zdevs.intellij.c.debug

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTaskManager
import com.intellij.task.impl.ModuleBuildTaskImpl


class CRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String {
        return type.id
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        val config = CRunConfiguration(project, this, "CDAP")
        val projectDir = project.guessProjectDir()?.path
        if (projectDir != null)
            config.workingDirectory = projectDir
        return config;
    }

    override fun getOptionsClass(): Class<out BaseState> {
        return CRunConfigurationOptions::class.java
    }

    override fun isEditableInDumbMode(): Boolean {
        return true
    }
}