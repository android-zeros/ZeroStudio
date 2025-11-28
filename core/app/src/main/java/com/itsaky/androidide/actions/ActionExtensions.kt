package com.itsaky.androidide.actions

import android.content.Context
import com.itsaky.androidide.ui.CodeEditorView
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File
import java.nio.file.Path

fun ActionData.getContext(): Context? {
    return get(Context::class.java)
}

fun ActionData.requireContext(): Context {
    return getContext() ?: throw IllegalArgumentException("No context instance provided")
}

fun ActionData.getEditorView(): CodeEditorView? {
    return get(CodeEditorView::class.java)
}

fun ActionData.getEditor(): CodeEditor? {
    return getEditorView()?.editor
}

fun ActionData.requireEditor(): CodeEditor {
    return getEditor() ?: throw IllegalArgumentException("An editor instance is required but none was provided")
}

fun ActionData.requireFile(): File {
    return get(File::class.java) ?: throw IllegalArgumentException("No file instance provided")
}

fun ActionData.requirePath(): Path {
    return requireFile().toPath()
}

/** Marks this action item as invisible and disabled. */
fun ActionItem.markInvisible() {
    visible = false
    enabled = false
}