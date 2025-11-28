package com.itsaky.androidide.actions.cursor

import android.content.Context
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.ActionItem
import com.itsaky.androidide.actions.ActionMenu
import com.itsaky.androidide.actions.EditorRelatedAction
import com.itsaky.androidide.actions.getEditor
import com.itsaky.androidide.actions.markInvisible

/**
 * An [ActionMenu] that consolidates all cursor movement operations for the text editor.
 * This action serves as a container for a sub-menu under the title "Cursor".
 */
class CursorActionsMenu(context: Context, override val order: Int) : EditorRelatedAction(), ActionMenu {


    override val id: String = "ide.editor.cursor.menu"
    
    override val children: MutableSet<ActionItem> = mutableSetOf()

    init {
        label = context.getString(R.string.action_group_cursor_title)
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_cursor_move)

        // Add all child cursor actions here, grouped logically
        // Basic Movement
        addAction(MoveUpAction(context, 10))
        addAction(MoveDownAction(context, 20))
        addAction(MoveLeftAction(context, 30))
        addAction(MoveRightAction(context, 40))

        // Line Boundaries
        addAction(GoToLineStartAction(context, 50))
        addAction(GoToLineEndAction(context, 60))

        // Word Boundaries
        addAction(GoToPreviousWordAction(context, 70))
        addAction(GoToNextWordAction(context, 80))
        
        // Page and Document Boundaries
        addAction(GoToPageUpAction(context, 90))
        addAction(GoToPageDownAction(context, 100))
        addAction(GoToDocumentStartAction(context, 110))
        addAction(GoToDocumentEndAction(context, 120))

        // Smart Navigation (The creative part)
        // addAction(GoToSymbolAction(context, 200))
    }

    override fun prepare(data: ActionData) {
        super<EditorRelatedAction>.prepare(data)
        super<ActionMenu>.prepare(data)

        if (data.getEditor() == null) {
            markInvisible()
        } else {
            visible = true
            enabled = true
        }
    }

    override suspend fun execAction(data: ActionData): Boolean {
        // This is a menu, so it doesn't perform an action itself.
        return true
    }
}