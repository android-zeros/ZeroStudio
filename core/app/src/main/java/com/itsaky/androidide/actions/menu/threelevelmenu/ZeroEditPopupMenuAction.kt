package com.itsaky.androidide.actions.menu.threelevelmenu

import android.content.Context
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorRelatedAction
import com.itsaky.androidide.editor.ui.IDEEditor
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.actions.menu.EditorLineOperations
import java.io.File

/**
 * 菜单容器Action
 */
class ZeroEditPopupMenuAction(context: Context, override val order: Int) : EditorRelatedAction() {
    override val id: String = "ide.editor.threelevelmenu"

    init {
        label = context.getString(R.string.delete_line)
        icon = ContextCompat.getDrawable(context, R.drawable.ic_edit_empty_line)
    }

    override suspend fun execAction(data: ActionData): Boolean {
        val context = data.get(Context::class.java) ?: return false
        val editorView = data.get(IDEEditor::class.java) ?: return false
        
        // 获取触发菜单的视图（这里使用编辑器视图作为锚点）
        val anchorView = editorView as View
        
        val popup = PopupMenu(context, anchorView)
        popup.menu.apply {
            // 添加item
            add(0, 1, 0, "三级菜单-复制代码块").setIcon(R.drawable.ic_edit_empty_line)
            add(0, 2, 1, "三级菜单-格式化代码").setIcon(R.drawable.ic_edit_empty_line)
            add(0, 3, 2, "三级菜单-生成注释").setIcon(R.drawable.ic_edit_empty_line)
        }
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
            //设置点击事件
                1 -> ThirdLevelCopyAction.execute(data)
                2 -> ThirdLevelFormatAction.execute(data)
                3 -> ThirdLevelCommentAction.execute(data)
            }
            true
        }
        
        popup.show()
        return true
    }
}

/**
 * 三级菜单子项 - 复制代码块
 */
private object ThirdLevelCopyAction {
    fun execute(data: ActionData): Boolean {
        val editor = data.get(IDEEditor::class.java) ?: return false
        if (editor.cursor.isSelected) {
            editor.copyText()
            return true
        }
        return false
    }
}

/**
 * 三级菜单子项 - 格式化代码
 */
private object ThirdLevelFormatAction {
    fun execute(data: ActionData): Boolean {
        val editor = data.get(IDEEditor::class.java) ?: return false
        val file = data.get(File::class.java) ?: return false
        return EditorLineOperations.formatCode(editor, file)
    }
}

/**
 * 三级菜单子项 - 生成注释
 */
private object ThirdLevelCommentAction {
    fun execute(data: ActionData): Boolean {
        val editor = data.get(IDEEditor::class.java) ?: return false
        val file = data.get(File::class.java) ?: return false
        return EditorLineOperations.toggleComment(editor, file)
    }
}