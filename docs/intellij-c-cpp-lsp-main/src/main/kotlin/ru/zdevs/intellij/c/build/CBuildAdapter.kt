package ru.zdevs.intellij.c.build

import com.intellij.build.*
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.build.output.BuildOutputParser
import com.intellij.execution.actions.StopProcessAction
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ThreeState
import javax.swing.JComponent
import ru.zdevs.intellij.c.build.event.*
import java.io.File


open class CBuildAdapter(
    private val ctx: CBuildContext,
    module: Module,
    title: String,
    parser: BuildOutputParser
) : ProcessAdapter() {
    private val buildProgressListener = module.project.service<BuildViewManager>()

    private val workingDirectory = module.moduleFile?.parent?.path

    private val instantReader = BuildOutputInstantReaderImpl(
        ctx.buildId,
        ctx.buildId,
        buildProgressListener,
        listOf(parser)
    )

    init {
        val buildToolWindow = BuildContentManager.getInstance(module.project).orCreateToolWindow
        buildToolWindow.setAvailable(true, null)
        buildToolWindow.activate(null)

        val buildContentDescriptor = BuildContentDescriptor(null, null, object : JComponent() {}, "Build")
        val activateToolWindow = true
        buildContentDescriptor.isActivateToolWindowWhenAdded = activateToolWindow
        buildContentDescriptor.isActivateToolWindowWhenFailed = activateToolWindow
        buildContentDescriptor.isNavigateToError = ThreeState.YES

        val descriptor = DefaultBuildDescriptor(ctx.buildId, title, workingDirectory.toString(), System.currentTimeMillis())
            .withContentDescriptor { buildContentDescriptor }
            .withRestartAction(StopProcessAction("Stop", "Stop", ctx.processHandler))

        val buildStarted = StartBuildEventImpl(descriptor, "running...")
        buildProgressListener.onEvent(ctx.buildId, buildStarted)
    }

    override fun processTerminated(event: ProcessEvent) {
        instantReader.closeAndGetFuture().whenComplete { _, error ->
            val isSuccess = event.exitCode == 0 && ctx.errors.get() == 0
            onBuildOutputReaderFinish(isSuccess = isSuccess, event.exitCode, error)
        }
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        instantReader.append(StringUtil.convertLineSeparators(event.text))
    }

    private fun onBuildOutputReaderFinish(
        isSuccess: Boolean,
        errorCode: Int,
        error: Throwable?,
    ) {
        val (status, result) = when {
            isSuccess  -> "successful" to SuccessResultImpl()
            else       -> "failed" to FailureResultImpl(errorCode, error)
        }

        val buildFinished = FinishBuildEventImpl(
            ctx.buildId,
            null,
            System.currentTimeMillis(),
            status,
            result
        )
        buildProgressListener.onEvent(ctx.buildId, buildFinished)

        if (workingDirectory == null)
            return

        val targetDir = VfsUtil.findFileByIoFile(File(workingDirectory), true) ?: return
        VfsUtil.markDirtyAndRefresh(true, true, true, targetDir)
    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {

    }
}
