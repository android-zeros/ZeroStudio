package ru.zdevs.intellij.c.project.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import ru.zdevs.intellij.c.project.CProject


class CModuleBuilder(private val buildSystem: String) : ModuleBuilder() {

    override fun getModuleType(): ModuleType<*> {
        return CModuleType.INSTANCE
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)

        doAddContentEntry(modifiableRootModel)

        modifiableRootModel.apply {
            contentEntries.firstOrNull()?.apply {
                addSourceFolder("file://$contentEntryPath", false)
                if (buildSystem == CProject.BUILD_SYSTEM_MAKEFILE) {
                    addExcludeFolder("file://$contentEntryPath/obj")
                } else {
                    addExcludeFolder("file://$contentEntryPath/build")
                }
                addExcludePattern("*.o")
            }
        }
    }
}