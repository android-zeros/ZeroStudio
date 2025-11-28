package ru.zdevs.intellij.c.debug

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.whenItemSelectedFromUi
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withFileToTextConvertor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.redhat.devtools.lsp4ij.dap.configurations.DAPRunConfiguration
import java.io.File
import java.nio.file.Paths
import javax.swing.*


class CRunConfigurationEditor : SettingsEditor<DAPRunConfiguration>() {
    private val panel: JTabbedPane
    private val debugger: JComboBox<String>
    private val command: TextFieldWithBrowseButton
    private val workDir: TextFieldWithBrowseButton
    private val executable: TextFieldWithBrowseButton
    private val executableFileChooser: FileChooserDescriptor
    private val stopAtMain: JCheckBox
    private val arguments: JTextArea

    init {
        debugger = JComboBox<String>(SERVER_NAMES)
        command = TextFieldWithBrowseButton()
        workDir = TextFieldWithBrowseButton()
        executable = TextFieldWithBrowseButton()
        stopAtMain = JCheckBox("Stop at beginning of main function")
        arguments = JTextArea()

        debugger.whenItemSelectedFromUi { item ->
            command.text = SERVER_COMMAND[SERVER_IDS.indexOf(item)][0]
        }
        command.addBrowseFolderListener(TextBrowseFolderListener(
            FileChooserDescriptorFactory.createSingleFileDescriptor()
                .withFileFilter { file ->
                    file.name.contains(SERVER_FILTER[debugger.selectedIndex])
                }
                .withTitle("Select Debugger Executable")
        ))
        workDir.addBrowseFolderListener(TextBrowseFolderListener(
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select Working Directory")
        ))
        executableFileChooser = FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withTitle("Select Target Executable")
            .withFileFilter { file ->
                !SystemInfo.isWindows || file.extension == "exe"
            }.withFileToTextConvertor { file ->
                Paths.get(workDir.text).relativize(Paths.get(file.path)).toString()
            }
        executable.addBrowseFolderListener(TextBrowseFolderListener(executableFileChooser))
        arguments.lineWrap = true
        arguments.document.putProperty("filterNewlines", true);

        val content = FormBuilder.createFormBuilder()
            .addLabeledComponent("Debugger Type", debugger)
            .addLabeledComponent("Debugger", command)
            .addSeparator()
            .addLabeledComponent("Working directory", workDir)
            .addLabeledComponent("C/C++ Application", executable)
            .addLabeledComponent("", stopAtMain)
            .panel
        content.border = JBUI.Borders.empty(5, 10)

        val args = FormBuilder.createFormBuilder()
            .addComponentFillVertically(arguments, 0)
            .panel
        args.border = JBUI.Borders.empty(5, 10)

        panel = JTabbedPane()
        panel.add("Main", content)
        panel.add("Arguments", args)
    }

    override fun resetEditorFrom(configuration: DAPRunConfiguration) {
        var serverIndex = SERVER_IDS.indexOf(configuration.serverId)
        if (serverIndex == -1) {
            configuration.serverId = SERVER_IDS[0]
            configuration.serverName = SERVER_NAMES[0]
            serverIndex = 0
        }
        debugger.selectedIndex = serverIndex
        command.text = if (configuration.command.isNullOrEmpty()) SERVER_COMMAND[serverIndex][0] else configuration.command
        workDir.text = configuration.workingDirectory ?: ""
        executable.text = configuration.file ?: ""

        val cconfiguration = (configuration as CRunConfiguration).getRunConfigurationOptions()
        stopAtMain.isSelected = cconfiguration.stopAtMain
        arguments.text = cconfiguration.launchArguments

        workDir.whenTextChanged {
            executableFileChooser.withRoots(LocalFileSystem.getInstance().findFileByIoFile(File(workDir.text)))
        }
        executableFileChooser.withRoots(LocalFileSystem.getInstance().findFileByIoFile(File(workDir.text)))
    }

    override fun applyEditorTo(configuration: DAPRunConfiguration) {
        configuration.serverId = SERVER_IDS[debugger.selectedIndex]
        configuration.command = command.text
        configuration.workingDirectory = workDir.text
        configuration.file = executable.text

        val cconfiguration = (configuration as CRunConfiguration).getRunConfigurationOptions()
        cconfiguration.stopAtMain = stopAtMain.isSelected
        cconfiguration.launchArguments = arguments.text
    }

    override fun createEditor(): JComponent {
        return panel
    }

    companion object {
        val SERVER_IDS = arrayOf("lldbDAPServer", "gdbDAPServer") // see plugin.xml
        val SERVER_NAMES = arrayOf("LLDB", "GDB")
        val SERVER_FILTER = arrayOf("lldb", "gdb") // app binary name
        val SERVER_COMMAND = arrayOf(LLDBDebugAdapterDescriptor.DEBUGGER_EXEC_NAME, GDBDebugAdapterDescriptor.DEBUGGER_EXEC_NAME)
    }
}