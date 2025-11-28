// by android_zero
package com.itsaky.androidide.actions.code

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.itsaky.androidide.actions.*
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.viewmodel.EditorViewModel
import java.io.File
import com.itsaky.androidide.actions.code.*
import com.itsaky.androidide.actions.menu.*


/**
 * @param context The application context, used for retrieving resources.
 * @param order The order of this action in menus or toolbars.
 */
class CodeActionsMenu(context: Context, override val order: Int) : EditorRelatedAction(), ActionMenu {

    /**
     * A mutable set holding all the child [ActionItem]s for this menu.
     */
    override val children: MutableSet<ActionItem> = mutableSetOf()

    /**
     * Initializes the menu action by setting its label and icon, and registering all
     * its child actions.
     */
    init {
        label = context.getString(R.string.edit)
        icon = ContextCompat.getDrawable(context, R.drawable.ic_edit)
        
                // addAction(OverrideMethodsAction(context, 1))
                // addAction(JavaToKotlinAction(context, 2))
        
    }

    /**
     * The unique identifier for the action.
     */
    override val id: String = "ide.editor.code.text.code_menu"

    /**
     * Prepares the action by updating its state based on the current context.
     * This action is made invisible if no editor is available.
     *
     * @param data The [ActionData] object containing data required for the action.
     */
    override fun prepare(data: ActionData) {
        super<EditorRelatedAction>.prepare(data) // Call super for EditorRelatedAction
        super<ActionMenu>.prepare(data) // Call super for ActionMenu

        if (!visible) {
            return
        }

        val editor = data.getEditor() ?: run {
            markInvisible()
            return
        }

        // The "Edit" menu itself should be visible if an editor exists.
        // Child actions will handle their own enabled state.
        enabled = true
    }

    /**
     * Executes the action. For an [ActionMenu], this method is a no-op as the framework
     * is responsible for displaying the sub-menu.
     *
     * @return `true` to indicate the action was handled.
     */
    override suspend fun execAction(data: ActionData): Boolean {
        Log.d("EditorEditLineMenuAction", "execAction called. Framework should handle sub-menu display.")
        return true
    }
}
