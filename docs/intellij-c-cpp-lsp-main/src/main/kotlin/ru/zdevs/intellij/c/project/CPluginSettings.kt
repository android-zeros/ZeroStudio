package ru.zdevs.intellij.c.project

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "CPluginSettings",
    storages = [Storage("CPluginSettings.xml")],
    category = SettingsCategory.PLUGINS
)
class CPluginSettings : PersistentStateComponent<CPluginSettings> {
    var languageType: String = CProject.LANGUAGE_C
    var buildSystem: String = CProject.BUILD_SYSTEM_MAKEFILE
    var toolChain: ArrayList<String> = ArrayList()

    override fun getState(): CPluginSettings {
        return this
    }

    override fun loadState(state: CPluginSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: CPluginSettings
            get() = ApplicationManager.getApplication().getService<CPluginSettings>(
                CPluginSettings::class.java
            )
    }
}