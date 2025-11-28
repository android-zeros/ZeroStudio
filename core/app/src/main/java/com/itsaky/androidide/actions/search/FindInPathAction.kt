package com.itsaky.androidide.actions.search

import android.content.Context
import androidx.core.content.ContextCompat
import com.itsaky.androidide.R
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.ActionItem
import com.itsaky.androidide.actions.EditorActivityAction
import com.itsaky.androidide.fragments.sheets.FindInPathDialog

/**
 * 用于打开高级搜索对话框的 Action
 * @author android_zero
 */
class FindInPathAction(context: Context, override val order: Int) : EditorActivityAction() {

    override val id: String = "ide.action.find.in.path"

    override var label: String = context.getString(R.string.menu_find_in_path)

    override var icon = ContextCompat.getDrawable(context, R.drawable.ic_search)

    override var location: ActionItem.Location = ActionItem.Location.EDITOR_TOOLBAR

    override var requiresUIThread: Boolean = true

    override suspend fun execAction(data: ActionData): Any {
        val activity = data.getActivity() ?: return false
        
        if (activity.supportFragmentManager.findFragmentByTag(TAG_DIALOG) != null) {
            return false
        }

        val dialog = FindInPathDialog()
        dialog.show(activity.supportFragmentManager, TAG_DIALOG)
        return true
    }

    companion object {
        private const val TAG_DIALOG = "FindInPathDialog"
    }
}