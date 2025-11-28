package com.itsaky.androidide.actions.menu

import android.content.Context
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorRelatedAction
import com.itsaky.androidide.resources.R

class ShrinkSelectionAction(context: Context, override val order: Int) : EditorRelatedAction() {

    override val id: String = "ide.editor.smart_select.shrink"

    init {
        label = context.getString(R.string.action_shrink_selection) 
    }
    
    override fun prepare(data: ActionData) {
        super.prepare(data)
        enabled = data.getEditor() != null
    }

    override suspend fun execAction(data: ActionData): Boolean {
        val editor = data.getEditor() ?: return false
        return EditorLineOperations.shrinkSelection(editor)
    }
        fun shrinkSelection(editor: IDEEditor): Boolean {
        val stack = getSmartSelectionStack(editor)
        if (stack.size <= 1) {
            if (stack.isNotEmpty()) {
                val lastKnownRange = stack.first()
                stack.clear()
                editor.setSelection(lastKnownRange.start)
            }
            return false
        }
        stack.removeAt(stack.lastIndex)
        val previousRange = stack.last()
        editor.setSelection(previousRange)
        return true
    }
}
