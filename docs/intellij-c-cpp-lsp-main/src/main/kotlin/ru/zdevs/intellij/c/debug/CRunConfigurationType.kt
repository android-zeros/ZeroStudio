package ru.zdevs.intellij.c.debug

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import ru.zdevs.intellij.c.Icons


class CRunConfigurationType : ConfigurationTypeBase(ID, "C / C++", "", Icons.C) {

    init {
        addFactory(CRunConfigurationFactory(this));
    }

    fun getInstance(): CRunConfigurationType {
        return findConfigurationType(CRunConfigurationType::class.java)
    }

    companion object {
        val ID = "CRunConfigurationType"
    }
}