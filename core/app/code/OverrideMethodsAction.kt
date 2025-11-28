package com.itsaky.androidide.actions.code

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorRelatedAction
import com.itsaky.androidide.actions.requireEditor
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.projects.ModuleProject
import com.itsaky.androidide.tasks.launchAsyncWithProgress
import com.itsaky.androidide.utils.DialogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Action to override or implement methods from superclasses or interfaces.
 */
class OverrideMethodsAction(private val context: Context, override val order: Int) : EditorRelatedAction() {
    init {
        id = "ide.editor.generate.override_methods"
        label = context.getString(R.string.action_override_methods)
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_override_method)
    }

    override fun prepare(data: ActionData) {
        super.prepare(data)
        if (!visible) return

        val file = data.get(File::class.java)
        val extension = file?.extension?.lowercase()
        visible = extension == "kt" || extension == "java"
        enabled = visible
    }

    override suspend fun execAction(data: ActionData): Boolean {
        val editor = data.requireEditor()
        val activity = editor.context as? AppCompatActivity ?: return false
        val file = data.get(File::class.java) ?: return false
        val module = IProjectManager.getInstance().requireWorkspace().findModuleForFile(file) as? ModuleProject

        activity.lifecycleScope.launchAsyncWithProgress(
            configureFlashbar = { builder, _ ->
                builder.message(R.string.action_override_parsing)
            }
        ) { _, _ ->
            val code = editor.text.toString()
            val cursorOffset = editor.cursor.left
            val classpaths = module?.getCompileClasspaths() ?: emptySet()

            val members = withContext(Dispatchers.IO) {
                PsiSymbolResolver.findOverridableMembers(file.name, code, cursorOffset, classpaths)
            }

            withContext(Dispatchers.Main) {
                if (members.isNotEmpty()) {
                    showOverrideDialog(activity, members)
                } else {
                    // Optionally, show a toast if no members are found
                }
            }
        }

        return true
    }

    private fun showOverrideDialog(context: Context, members: List<OverridableMember>) {
        val editor = requireEditor()
        val displayItems = members.map { it.signature }.toTypedArray()
        val checkedItems = BooleanArray(members.size)

        DialogUtils.newMaterialDialogBuilder(context)
            .setTitle(R.string.action_override_select_methods)
            .setMultiChoiceItems(displayItems, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val selectedMembers = members.filterIndexed { index, _ -> checkedItems[index] }
                if (selectedMembers.isNotEmpty()) {
                    val generatedCode = CodeGenerator.generateOverrideMethods(selectedMembers)
                    val ktClass = selectedMembers.first().containingClass
                    // Insert before the closing brace of the class/object
                    val insertOffset = ktClass.body?.rBrace?.textOffset ?: (ktClass.textOffset + ktClass.textLength)
                    
                    val pos = editor.text.indexer.getCharPosition(insertOffset)
                    
                    // Add some newlines for spacing if needed
                    val textToInsert = "\n\n$generatedCode"
                    
                    editor.text.insert(pos.line, pos.column, textToInsert)
                    // Optionally format the code after insertion
                    // editor.formatCodeAsync()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}