package ru.zdevs.intellij.c.build

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.task.*
import com.intellij.util.EnvironmentUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.concurrency.resolvedPromise
import ru.zdevs.intellij.c.project.CProject
import ru.zdevs.intellij.c.project.module.CModuleSettings
import ru.zdevs.intellij.c.project.module.CModuleType
import java.awt.EventQueue.invokeLater
import java.io.File
import java.nio.file.Paths


class CBuildProjectTaskRunner : ProjectTaskRunner() {
    private fun findBearExecutableInPATH() =
        EnvironmentUtil.getValue("PATH")?.split(File.pathSeparator)?.firstNotNullOfOrNull { path ->
            Paths.get(path, BEAR_EXEC_NAME).toFile().takeIf { it.canExecute() }
        }?.path

    override fun canRun(projectTask: ProjectTask): Boolean {
        if (projectTask is ModuleBuildTask) {
            return projectTask.module.moduleTypeName == CModuleType.ID
        }
        return false
    }

    override fun run(project: Project, context: ProjectTaskContext, vararg tasks: ProjectTask): Promise<Result> {
        if (project.isDisposed) {
            return rejectedPromise("Project is already disposed")
        }

        ApplicationManager.getApplication().invokeAndWait({
                FileDocumentManager.getInstance().saveAllDocuments()
            },
            ModalityState.defaultModalityState()
        )

        val resultPromise = AsyncPromise<Result>()

        tasks.forEach { task ->
            if (task !is ModuleBuildTask) {
                return resolvedPromise(TaskRunnerResults.ABORTED)
            }

            invokeLater {
                start(task.module, resultPromise, !task.isIncrementalBuild)
            }
        }

        return resultPromise
    }

    private fun getCommandLine(workDirectory: String?) : GeneralCommandLine {
        return PtyCommandLine()
            .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
            .withConsoleMode(true)
            .withEnvironment("LANG", "en_US.UTF-8")
            .withEnvironment("LANGUAGE", "en")
            .withWorkDirectory(workDirectory)
    }

    private fun startConfigure(module: Module, workDirectory: String?, resultPromise: AsyncPromise<Result>) {
        val commandLine = getCommandLine(workDirectory)
            .withExePath("cmake")
            .withParameters("..")

        val handler = BlockedProcessHandler(commandLine)
        val ctx = CBuildContext(handler)
        handler.addProcessListener(CBuildAdapter(ctx, module, "Run CMake", CMakeBuildParser(ctx, module.moduleFile?.parent?.path)))
        handler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                if (event.exitCode == 0) {
                    resultPromise.setResult(TaskRunnerResults.SUCCESS)

                    startBuild(module, workDirectory, resultPromise, true)
                } else {
                    resultPromise.setResult(TaskRunnerResults.FAILURE)
                }
            }
        })
        handler.startNotify()
    }

    private fun startBuild(module: Module, workDirectory: String?, resultPromise: AsyncPromise<Result>, cmake: Boolean) {
        val commandLine = getCommandLine(workDirectory)
        if (cmake) {
            commandLine.withExePath("cmake")
                .withParameters("--build", ".")
        } else {
            val bear  = findBearExecutableInPATH()
            if (bear != null) {
                // use bear for compile_commands.json generation
                commandLine.withExePath("bear")
                    .withParameters("--append", "--", "make", "all")
            } else {
                commandLine.withExePath("make")
                    .withParameters("all")
            }
        }

        val handler = BlockedProcessHandler(commandLine)
        val ctx = CBuildContext(handler)
        handler.addProcessListener(CBuildAdapter(ctx, module, "Run C/C++ Build", MakeBuildParser(ctx, workDirectory)))
        handler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                if (event.exitCode == 0) {
                    resultPromise.setResult(TaskRunnerResults.SUCCESS)
                } else {
                    resultPromise.setResult(TaskRunnerResults.FAILURE)
                }
            }
        })
        handler.startNotify()
    }

    private fun startClean(module: Module, resultPromise: AsyncPromise<Result>, settings: CModuleSettings) {
        val workDirectory = module.moduleFile?.parent?.path

        val commandLine = getCommandLine(workDirectory)
        if (settings.buildSystem == CProject.BUILD_SYSTEM_CMAKE) {
            commandLine.withExePath("cmake")
                .withParameters("--build", "build", "--target", "clean")
        } else {
            commandLine.withExePath("make")
                .withParameters("clean")
        }

        val handler = BlockedProcessHandler(commandLine)
        val ctx = CBuildContext(handler)
        handler.addProcessListener(CBuildAdapter(ctx, module, "Run clean", CMakeBuildParser(ctx, workDirectory)))
        handler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                if (event.exitCode == 0) {
                    resultPromise.setResult(TaskRunnerResults.SUCCESS)

                    val compileCommands = File(workDirectory, "compile_commands.json")
                    if (compileCommands.exists()) compileCommands.delete()

                    start(module, resultPromise, false)
                } else {
                    resultPromise.setResult(TaskRunnerResults.FAILURE)
                }
            }
        })
        handler.startNotify()
    }

    private fun start(module: Module, resultPromise: AsyncPromise<Result>, cleanProject: Boolean) {
        val settings = module.getService(CModuleSettings::class.java)

        if (settings.buildSystem == CProject.BUILD_SYSTEM_CMAKE) {
            val workDirectory = module.moduleFile?.parent?.path + "/build"
            val dir = File(workDirectory)

            if (cleanProject && dir.exists()) {
                startClean(module, resultPromise, settings)
                return
            }

            if (!dir.exists()) dir.mkdirs()
            startConfigure(module, workDirectory, resultPromise)
        } else {
            if (cleanProject) {
                startClean(module, resultPromise, settings)
                return
            }

            startBuild(module, module.moduleFile?.parent?.path, resultPromise, false)
        }
    }

    companion object {
        val BEAR_EXEC_NAME = if (SystemInfo.isWindows) "bear.exe" else "bear"
    }
}
