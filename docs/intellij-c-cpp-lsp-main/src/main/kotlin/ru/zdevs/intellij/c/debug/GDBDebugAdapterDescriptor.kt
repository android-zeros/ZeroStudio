package ru.zdevs.intellij.c.debug

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.SystemInfo
import com.redhat.devtools.lsp4ij.dap.DebugMode
import com.redhat.devtools.lsp4ij.dap.client.LaunchUtils
import com.redhat.devtools.lsp4ij.dap.client.LaunchUtils.LaunchContext
import com.redhat.devtools.lsp4ij.dap.configurations.DAPRunConfigurationOptions
import com.redhat.devtools.lsp4ij.dap.configurations.options.FileOptionConfigurable
import com.redhat.devtools.lsp4ij.dap.configurations.options.WorkingDirectoryConfigurable
import com.redhat.devtools.lsp4ij.dap.definitions.DebugAdapterServerDefinition
import com.redhat.devtools.lsp4ij.dap.descriptors.DefaultDebugAdapterDescriptor
import com.redhat.devtools.lsp4ij.dap.descriptors.ServerReadyConfig
import ru.zdevs.intellij.c.Utils


class GDBDebugAdapterDescriptor(
    options: RunConfigurationOptions,
    environment: ExecutionEnvironment,
    serverDefinition: DebugAdapterServerDefinition) :
    DefaultDebugAdapterDescriptor(options, environment, serverDefinition) {

    @Throws(ExecutionException::class)
    override fun startServer(): ProcessHandler {
        var command =  if (options is DAPRunConfigurationOptions) {
            (options as DAPRunConfigurationOptions).command
        } else
            null
        if (command.isNullOrEmpty() || LLDBDebugAdapterDescriptor.DEBUGGER_EXEC_NAME.contains(command)) {
            command = Utils.findExecutableInPATH(DEBUGGER_EXEC_NAME)
        }
        if (command.isNullOrEmpty()) {
            throw ExecutionException("GDB not found. Make sure it is installed properly (and `${DEBUGGER_EXEC_NAME[0]}` available in PATH), and restart the IDE.")
        }

        val file = (options as FileOptionConfigurable).file
        if (file.isNullOrEmpty()) {
            throw ExecutionException("Debuggable file is not specified. Make sure it is specified at the configuration tab of the Run/Debug configuration dialog.")
        }

        command += " -i dap"
        val commandLine: GeneralCommandLine = createStartServerCommandLine(command)
        return startServer(commandLine)
    }

    override fun getDapParameters(): Map<String, Any> {
        val stopAtMain: Boolean
        val launchArguments: List<String>
        if (options is CRunConfigurationOptions) {
            with (options as CRunConfigurationOptions) {
                stopAtMain = this.stopAtMain
                launchArguments = getLaunchArgumentJSONList()
            }
        } else {
            stopAtMain = true
            launchArguments = listOf()
        }

        // language=JSON
        val launchJson = """
                {
                  "type": "gdb-dap",
                  "name": "Launch executable file",
                  "request": "launch",
                  "program": "${'$'}{workspaceFolder}/${'$'}{file}",
                  "args": $launchArguments,
                  "cwd": "${'$'}{workspaceFolder}",
                  "stopAtBeginningOfMainSubprogram": $stopAtMain
                }
                """.trimIndent()
        val file = (options as FileOptionConfigurable).file
        val workspaceFolder = (options as WorkingDirectoryConfigurable).workingDirectory
        val context = LaunchContext(file, workspaceFolder)
        return LaunchUtils.getDapParameters(launchJson, context)
    }

    override fun getServerReadyConfig(debugMode: DebugMode): ServerReadyConfig {
        return ServerReadyConfig(0)
    }

    override fun getFileType(): FileType? {
        return null
    }

    companion object {
        val DEBUGGER_EXEC_NAME = if (SystemInfo.isWindows) {
            arrayOf("gdb.exe")
        } else {
            arrayOf("gdb")
        }
    }
}