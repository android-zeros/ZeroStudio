/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.actions.sidebar

import android.content.Context
import android.zero.studio.terminal.TermuxFragment
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.itsaky.androidide.R
import kotlin.reflect.KClass

/**
 * Sidebar action for opening the terminal fragment directly in the sidebar.
 *
 * @author Akash Yadav
 * @author android_zero (Refactor)
 */
class TerminalSidebarAction(context: Context, override val order: Int) : AbstractSidebarAction() {

  companion object {
    const val ID = "ide.editor.sidebar.terminal"
  }

  override val id: String = ID

  override val fragmentClass: KClass<out Fragment> = TermuxFragment::class

  init {
    label = context.getString(R.string.title_terminal)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_terminal)
  }
  }