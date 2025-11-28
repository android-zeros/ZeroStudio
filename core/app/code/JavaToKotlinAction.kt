package com.itsaky.androidide.actions.code

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorRelatedAction
import com.itsaky.androidide.actions.getEditor
import com.itsaky.androidide.actions.markInvisible
import com.itsaky.androidide.actions.requireEditor
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.projects.ModuleProject
import com.itsaky.androidide.tasks.launchAsyncWithProgress
import com.itsaky.androidide.utils.DialogUtils
import com.itsaky.androidide.utils.flashError
import com.itsaky.androidide.utils.flashSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Action to convert the current Java file to Kotlin.
 */
class JavaToKotlinAction(private val context: Context, override val order: Int) : EditorRelatedAction() {

    override val id: String = "ide.editor.convert.java2kotlin"

    init {
        label = context.getString(R.string.action_java_to_kotlin)
        icon = ContextCompat.getDrawable(context, R.drawable.ic_java_to_kotlin)
    }

    override fun prepare(data: ActionData) {
        super.prepare(data)
        if (!visible) return

        val file = data.get(File::class.java)
        visible = file?.extension?.equals("java", ignoreCase = true) == true
        enabled = visible
    }

    override suspend fun execAction(data: ActionData): Boolean {
        val editor = data.requireEditor()
        val activity = editor.context as? AppCompatActivity ?: return false
        val file = data.get(File::class.java) ?: return false
        val module = IProjectManager.getInstance().requireWorkspace().findModuleForFile(file) as? ModuleProject

        activity.lifecycleScope.launchAsyncWithProgress(
            configureFlashbar = { builder, _ ->
                builder.message(R.string.action_java_to_kotlin_converting)
            }
        ) { _, _ ->
            val code = editor.text.toString()
            val classpaths = module?.getCompileClasspaths() ?: emptySet()

            val convertedCode = withContext(Dispatchers.IO) {
                J2kConverterHelper.convert(code, classpaths)
            }

            withContext(Dispatchers.Main) {
                if (convertedCode.isNullOrBlank()) {
                    activity.flashError(R.string.action_java_to_kotlin_failed)
                } else {
                    val newFile = File(file.parent, "${file.nameWithoutExtension}.kt")
                    try {
                        withContext(Dispatchers.IO) {
                            newFile.writeText(convertedCode)
                        }
                        activity.flashSuccess(
                            activity.getString(R.string.action_java_to_kotlin_success, newFile.name)
                        )
                        showOpenFileDialog(activity, newFile)
                    } catch (e: Exception) {
                        activity.flashError(activity.getString(R.string.action_java_to_kotlin_failed_write, e.message))
                    }
                }
            }
        }
        return true
    }

    private fun showOpenFileDialog(activity: AppCompatActivity, file: File) {
        DialogUtils.newYesNoDialog(
            activity,
            activity.getString(R.string.action_java_to_kotlin_success_title),
            activity.getString(R.string.action_java_to_kotlin_open_file),
            positiveClickListener = { dialog, _ ->
                (activity as? com.itsaky.androidide.interfaces.IEditorHandler)?.doOpenFile(file)
                dialog.dismiss()
            },
            negativeClickListener = { dialog, _ ->
                dialog.dismiss()
            }
        ).show()
    }
}