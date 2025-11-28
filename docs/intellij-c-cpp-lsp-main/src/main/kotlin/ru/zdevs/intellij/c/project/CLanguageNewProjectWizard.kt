package ru.zdevs.intellij.c.project

import com.intellij.history.core.Paths
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.ide.wizard.setupProjectFromBuilder
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.util.containers.toArray
import ru.zdevs.intellij.c.Icons
import ru.zdevs.intellij.c.project.module.CModuleBuilder
import ru.zdevs.intellij.c.project.module.CModuleSettings
import java.nio.file.Path
import java.util.*
import javax.swing.JComboBox

class CLanguageNewProjectWizard : LanguageGeneratorNewProjectWizard {

    override val icon = Icons.C

    override val name = "C / C++"

    override val ordinal = 100

    override fun createStep(parent: NewProjectWizardStep) = Step(parent)

    class Step(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
        private lateinit var languageType: SegmentedButton<String>
        private lateinit var buildSystem: SegmentedButton<String>
        private lateinit var toolChain: ComboBox<String>

        override fun setupUI(builder: Panel) {

            val state = CPluginSettings.instance.state
            val toolChainList = mutableListOf("clang", "gcc")
            toolChainList.addAll(state.toolChain)

            toolChain = ComboBox<String>(toolChainList.toArray(arrayOf()))

            with(builder) {
                row("Build system:") {
                    buildSystem = segmentedButton(listOf(CProject.BUILD_SYSTEM_MAKEFILE, CProject.BUILD_SYSTEM_CMAKE)) { text = it }
                }
                row("Language:") {
                    languageType = segmentedButton(listOf(CProject.LANGUAGE_C, CProject.LANGUAGE_CPP)) { text = it }
                }
                row("Compiler:") {
                    val ui = cell(ComponentWithBrowseButton<ComboBox<String>>(toolChain, {})).align(Align.FILL).component

                    val choose = FileChooserDescriptor(true, false, false, false, false, false)
                    choose.withTreeRootVisible(true)
                    choose.withFileFilter { file ->
                        file.name.endsWith("gcc$EXEC_EXTENSION") or file.name.endsWith("g++$EXEC_EXTENSION") or file.name.startsWith("clang")
                    }
                    val folderHandling: TextComponentAccessor<JComboBox<String>> =
                        object : TextComponentAccessor<JComboBox<String>> {
                            override fun getText(comboBox: JComboBox<String>): String {
                                val path = comboBox.selectedItem as String
                                return if (path.startsWith("/")) path else "/"
                            }

                            override fun setText(comboBox: JComboBox<String>, path: String) {
                                comboBox.addItem(path)
                                comboBox.selectedItem = path
                            }
                        }
                    ui.addBrowseFolderListener(null, null, null, choose, folderHandling)
                }
            }

            languageType.selectedItem = state.languageType
            buildSystem.selectedItem = state.buildSystem
        }

        override fun setupProject(project: Project) {
            Objects.requireNonNull(project.basePath, "Project base path cannot be null")
            Objects.requireNonNull(project.name, "Project name cannot be null")

            val module = setupProjectFromBuilder(project, CModuleBuilder(buildSystem.selectedItem!!))
            Objects.requireNonNull(module, "Project module cannot be null")

            val settings = module!!.getService(CModuleSettings::class.java)
            Objects.requireNonNull(settings, "Project module settings cannot be null")
            settings.buildSystem = buildSystem.selectedItem!!

            val modifiableModel = ModuleRootManager.getInstance(module).modifiableModel
            modifiableModel.commit()

            val state = CPluginSettings.instance.state

            val properties = Properties()
            properties.setProperty("NAME", project.name + EXEC_EXTENSION)

            val templateManager = FileTemplateManager.getInstance(project)
            val templateMainName = if (languageType.selectedItem == CProject.LANGUAGE_CPP) "main.cpp" else "main.c"
            val templateMain = templateManager.getInternalTemplate(templateMainName)
            val mainFile = Path.of(project.basePath, templateMainName).toFile()
            FileUtil.writeToFile(mainFile, templateMain.getText(properties))

            var toolchain = ""
            var cc: String
            val cxx: String

            val path = toolChain.selectedItem as String
            if (path.startsWith("/") || path.contains(":\\")) {
                if (path.endsWith("gcc$EXEC_EXTENSION") || path.endsWith("g++$EXEC_EXTENSION")) {
                    toolchain = path.substring(0, path.length - 3 - EXEC_EXTENSION.length)
                    cc = "gcc$EXEC_EXTENSION"
                    cxx = "g++$EXEC_EXTENSION"
                } else {
                    toolchain = Paths.getParentOf(toolchain) ?: ""
                    cc = Paths.getNameOf(toolchain)
                    if (cc.startsWith("clang++")) {
                        cxx = cc
                        cc = cxx.replace("clang++", "clang")
                    } else {
                        cxx = cc.replace("clang", "clang++")
                    }
                }
            } else {
                cc = path + EXEC_EXTENSION
                cxx = if (path == "gcc") "g++$EXEC_EXTENSION" else "clang++$EXEC_EXTENSION"
            }

            val ld = if (languageType.selectedItem == CProject.LANGUAGE_CPP) cxx else cc

            properties.setProperty("TOOLCHAIN", toolchain)
            properties.setProperty("CC", cc)
            properties.setProperty("CXX", cxx)
            properties.setProperty("LD", ld)
            properties.setProperty("SOURCES", templateMainName)

            val templateMakeName = if (buildSystem.selectedItem == CProject.BUILD_SYSTEM_MAKEFILE) "Makefile" else "CMakeLists.txt"
            val templateMake = templateManager.getInternalTemplate(templateMakeName)
            FileUtil.writeToFile(Path.of(project.basePath, templateMakeName).toFile(), templateMake.getText(properties))

            FileUtil.writeToFile(Path.of(project.basePath, ".gitignore").toFile(),
                """
                *.o
                *.so
                *.exe
                *.dylib
                *.dll

                obj/
                build/

                .idea/

                # clangd
                compile_commands.json

                # OS-specific files
                .DS_Store
                .Trashes
                ehthumbs.db
                Thumbs.db
                .directory
                """.trimIndent()
            )

            if (buildSystem.selectedItem == CProject.BUILD_SYSTEM_CMAKE) {
                FileUtil.writeToFile(Path.of(project.basePath, "config.h.in").toFile(), "")
            }

            if (toolChain.selectedIndex > 1 && !state.toolChain.contains(toolChain.selectedItem as String))
                state.toolChain.add(toolChain.selectedItem as String)

            state.languageType = languageType.selectedItem ?: ""
            state.buildSystem = buildSystem.selectedItem ?: ""
        }
    }

    companion object {
        val EXEC_EXTENSION = if (SystemInfo.isWindows) ".exe" else ""
    }
}