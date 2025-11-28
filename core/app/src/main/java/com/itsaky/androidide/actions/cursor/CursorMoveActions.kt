package com.itsaky.androidide.actions.cursor

import android.content.Context
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorRelatedAction
import com.itsaky.androidide.actions.requireEditor
import io.github.rosemoe.sora.widget.SelectionMovement

// --- Basic Movement Actions ---

class MoveUpAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.move_up"
    init {
        label = context.getString(R.string.action_editor_line_cursor_move_up)
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_up)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.UP)
        return true
    }
}

class MoveDownAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.move_down"
    init {
        label = context.getString(R.string.action_editor_line_cursor_move_down)
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_down)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.DOWN)
        return true
    }
}

class MoveLeftAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.move_left"
    init {
        label = context.getString(R.string.action_editor_line_cursor_move_left)
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_left)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.LEFT)
        return true
    }
}

class MoveRightAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.move_right"
    init {
        label = context.getString(R.string.action_editor_line_cursor_move_right)
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_right)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.RIGHT)
        return true
    }
}

// --- Line Boundary Actions ---

class GoToLineStartAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.home"
    init {
        label = context.getString(R.string.action_editor_line_cursor_home)
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_line_start)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.LINE_START)
        return true
    }
}

class GoToLineEndAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.end"
    init {
        label = context.getString(R.string.action_editor_line_cursor_end)
        // icon = ContextCompat.getDrawable(context, R.drawable.ic_line_end)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.LINE_END)
        return true
    }
}

// --- Word Boundary Actions ---

class GoToPreviousWordAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.prev_word"
    init {
        label = context.getString(R.string.action_editor_cursor_prev_word)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.PREVIOUS_WORD_BOUNDARY)
        return true
    }
}

class GoToNextWordAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.next_word"
    init {
        label = context.getString(R.string.action_editor_cursor_next_word)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.NEXT_WORD_BOUNDARY)
        return true
    }
}

// --- Page and Document Boundary Actions ---

class GoToPageUpAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.page_up"
    init {
        label = context.getString(R.string.action_editor_cursor_page_up)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.PAGE_UP)
        return true
    }
}

class GoToPageDownAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.page_down"
    init {
        label = context.getString(R.string.action_editor_cursor_page_down)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.PAGE_DOWN)
        return true
    }
}

class GoToDocumentStartAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.doc_start"
    init {
        label = context.getString(R.string.action_editor_cursor_doc_start)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.TEXT_START)
        return true
    }
}

class GoToDocumentEndAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.doc_end"
    init {
        label = context.getString(R.string.action_editor_cursor_doc_end)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        data.requireEditor().moveSelection(SelectionMovement.TEXT_END)
        return true
    }
}