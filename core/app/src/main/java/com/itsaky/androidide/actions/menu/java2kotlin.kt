// by android_zero
package com.itsaky.androidide.actions.menu

import android.content.Context
import androidx.core.content.ContextCompat
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorRelatedAction
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * 中文注释: java 代码转为kotlin代码
 * English annotation: java code to kotlin code
 *
 *方法函数需要具备：
 *@file_get文件：文件绝对路径
 *@内容输入 输入文件全部java代码
 *@内容输出到文件 输出转换后的kotlin代码
 */
class java2kotlin(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.compile.java2kotlin"
    init {
        label = context.getString(R.string.java_to_kotlin)
        icon = ContextCompat.getDrawable(context, R.drawable.ic_edit_jump_to_line)
    }
    override suspend fun execAction(data: ActionData): Boolean {
        val editor = data.getEditor() ?: return false
        val context = data.get(Context::class.java) ?: return false
        return EditorLineOperations.jumpToLine(editor, context)
    }
}