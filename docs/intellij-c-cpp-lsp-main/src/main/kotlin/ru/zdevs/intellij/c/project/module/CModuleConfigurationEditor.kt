package ru.zdevs.intellij.c.project.module

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleConfigurationEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import ru.zdevs.intellij.c.project.CProject
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CModuleConfigurationEditor(val module: Module) : ModuleConfigurationEditor {
    private val panel: JPanel
    private val buildSystem: ComboBox<String>

    init {
        buildSystem = ComboBox<String>(arrayOf(CProject.BUILD_SYSTEM_MAKEFILE, CProject.BUILD_SYSTEM_CMAKE))

        val content = FormBuilder.createFormBuilder()
            .addLabeledComponent("Build system:", buildSystem)
            .panel

        panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(5, 10)
        panel.add(content, "North")
    }

    override fun getDisplayName(): String {
        return "Build"
    }

    override fun createComponent(): JComponent {
        val settings = module.getService(CModuleSettings::class.java)
        buildSystem.selectedItem = settings.buildSystem
        return panel
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {
        val settings = module.getService(CModuleSettings::class.java)
        settings.buildSystem = buildSystem.selectedItem as String
    }
}