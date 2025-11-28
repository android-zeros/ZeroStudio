package com.itsaky.androidide.actions.menu

import android.content.Context
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorRelatedAction
import com.itsaky.androidide.editor.utils.getCursorCount
import com.itsaky.androidide.resources.R

class SelectAllOccurrencesAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.cursor.select_all_occurrences"

    init {
        label = context.getString(R.string.action_select_all_occurrences)
    }

    override fun prepare(data: ActionData) {
        super.prepare(data)
        val editor = data.getEditor()
        enabled = editor != null && editor.cursor.getCursorCount() <= 1
    }

    override suspend fun execAction(data: ActionData): Boolean {
        val editor = data.getEditor() ?: return false
        return EditorLineOperations.selectAllOccurrences(editor)
    }
    
        fun selectAllOccurrences(editor: CodeEditor): Boolean {
        val cursor = editor.cursor
        if (cursor.getCursorCount() > 1) {
            return false
        }
        if (!cursor.isSelected) {
            editor.selectCurrentWord()
            if (!cursor.isSelected) {
                return false
            }
        }
        val selectedText = editor.text.subSequence(cursor.left().index, cursor.right().index).toString()
        if (selectedText.isNullOrEmpty()) {
            return false
        }
        val searcher = editor.searcher
        val searchOptions = EditorSearcher.SearchOptions(true, true)
        searcher.search(selectedText, searchOptions)
        val results = mutableListOf<io.github.rosemoe.sora.text.TextRange>()
        while (searcher.gotoNext()) {
            results.add(editor.cursor.range)
        }
        if (results.size <= 1) {
            searcher.stopSearch()
            return false
        }
        cursor.clearSelection()
        for (result in results) {
            cursor.addSelection(result.start.line, result.start.column, result.end.line, result.end.column)
        }
        searcher.stopSearch()
        return true
    }

}