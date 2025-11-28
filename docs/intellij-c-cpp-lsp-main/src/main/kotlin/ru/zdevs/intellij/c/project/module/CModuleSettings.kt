package ru.zdevs.intellij.c.project.module

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import ru.zdevs.intellij.c.project.CProject

@State(
    name = "CModuleSettings",
    storages = [Storage(StoragePathMacros.MODULE_FILE)],
)
class CModuleSettings : PersistentStateComponent<CModuleSettings> {
    var buildSystem: String = CProject.BUILD_SYSTEM_MAKEFILE

    override fun getState(): CModuleSettings {
        return this
    }

    override fun loadState(state: CModuleSettings) {
        XmlSerializerUtil.copyBean(state, this.state)
    }
}