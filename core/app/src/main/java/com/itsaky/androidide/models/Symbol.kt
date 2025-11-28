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
package com.itsaky.androidide.models

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * A symbol that is shown in the [SymbolInputView][com.itsaky.androidide.ui.SymbolInputView].
 * @author：android_zero  New content: Addition onCommit，onLongCommit
 * @author Akash Yadav
 */
open class Symbol @JvmOverloads constructor(
  open val label: String,
  open val commit: String,
  open val offset: Int = 1
) {
  @JvmOverloads
  constructor(both: String, offset: Int = 1) : this(both, both, offset)

  /**
   * Called when the symbol is clicked.
   * Default behavior is to insert the `commit` text.
   * This method is open and can be overridden by subclasses to provide custom behavior.
   *
   * @param editor The CodeEditor instance where the action should be performed.
   */
  open fun onCommit(editor: CodeEditor) {
    editor.insertText(commit, offset)
  }

  /**
   * Called when the symbol is long-clicked.
   * Default behavior is the same as a single click.
   * This method is open and can be overridden by subclasses.
   *
   * @param editor The CodeEditor instance where the action should be performed.
   */
  open fun onLongCommit(editor: CodeEditor) {
    onCommit(editor)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Symbol) return false

    if (label != other.label) return false
    if (commit != other.commit) return false
    if (offset != other.offset) return false

    return true
  }

  override fun hashCode(): Int {
    var result = label.hashCode()
    result = 31 * result + commit.hashCode()
    result = 31 * result + offset
    return result
  }

  override fun toString(): String {
    return "Symbol(label='$label', commit='$commit', offset=$offset)"
  }
}