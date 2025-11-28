package ru.zdevs.intellij.c.project.module

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleType
import ru.zdevs.intellij.c.project.CProject
import javax.swing.Icon


class CModuleType : ModuleType<CModuleBuilder>(ID) {
    override fun createModuleBuilder(): CModuleBuilder {
        return CModuleBuilder(CProject.BUILD_SYSTEM_MAKEFILE)
    }

    override fun getName(): String {
        return "C Module"
    }

    override fun getDescription(): String {
        return ""
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return AllIcons.Nodes.Module;
    }

    companion object {
        const val ID = "C_MODULE"

        val INSTANCE = CModuleType()
    }
}