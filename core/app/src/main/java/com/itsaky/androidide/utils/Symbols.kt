/*
 * This file is part of AndroidIDE.
 *
 * AndroidIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndroidIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package com.itsaky.androidide.utils

import com.itsaky.androidide.models.Symbol
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SelectionMovement
import java.io.File

object Symbols {

  @JvmStatic
  fun forFile(file: File?): List<Symbol> {
    if (file == null || !file.isFile) {
      return emptyList()
    }

    return when (file.extension) {
      "java",
      "gradle",
      "kt",
      "kts" -> javaSymbols

      "xml" -> xmlSymbols
      else -> plainTextSymbols
    }
  }

  private val javaSymbols by lazy {
    listOf(
      TabSymbol(),
      MoveLeftSymbol(),
      MoveUpSymbol(),
      MoveDownSymbol(),
      MoveRightSymbol(),
      
      Symbol("//"),
      Symbol("/*", "/*", 2),
      Symbol("*/", "*/", 2),
      JavaDocSymbol(),
      Symbol(","),
      Symbol("{", "{}"),
      Symbol("}"),
      Symbol("(", "()"),
      Symbol(")"),
      Symbol(";"),
      Symbol("="),
      Symbol("=="),
      Symbol("!="),
      Symbol("?="),
      Symbol("<="),
      Symbol(">="),
      Symbol("+="),
      Symbol("*="),
      Symbol("/="),
      Symbol("==="),
      Symbol("!=="),
      Symbol("\"", "\"\""),
      Symbol("|"),
      Symbol("$"),
      Symbol("%"),
      Symbol("@"),
      Symbol("#"),
      Symbol("&"),
      Symbol("&&"),
      Symbol("`"),
      Symbol("in"),
      Symbol("as"),
      Symbol("->"),
      Symbol("!"),
      Symbol("!!"),
      Symbol("?."),
      Symbol("?:"),
      Symbol("[", "[]"),
      Symbol("]"),
      Symbol("<", "<>"),
      Symbol(">"),
      Symbol("..<"),
      Symbol("+"),
      Symbol("++"),
      Symbol("--"),
      Symbol("/"),
      Symbol("*"),
      Symbol("?"),
      Symbol(":"),
      Symbol("::"),
      Symbol(";"),
      Symbol("_"),
      Symbol("new"),
      Symbol("null"),
      Symbol("@Override")
    )
  }

  private val xmlSymbols by lazy {
    listOf(
      TabSymbol(),
      MoveLeftSymbol(),
      MoveUpSymbol(),
      MoveDownSymbol(),
      MoveRightSymbol(),
      Symbol("<", "<>"),
      Symbol(">"),
      
      Symbol("<!--", "<!-- -->", 4),
      Symbol("-->", "-->", 3),
      
      Symbol("/"),
      Symbol("="),
      Symbol("\"", "\"\""),
      Symbol(":"),
      Symbol("@"),
      Symbol("+"),
      Symbol("(", "()"),
      Symbol(")"),
      Symbol(";"),
      Symbol(","),
      Symbol("."),
      Symbol("?"),
      Symbol("|"),
      Symbol("\\"),
      Symbol("&"),
      Symbol("[", "[]"),
      Symbol("]"),
      Symbol("{", "{}"),
      Symbol("}"),
      Symbol("_"),
      Symbol("-")
    )
  }

  val plainTextSymbols by lazy {
    listOf(
      TabSymbol(),
      MoveLeftSymbol(),
      MoveUpSymbol(),
      MoveDownSymbol(),
      MoveRightSymbol(),
      Symbol("{", "{}"),
      Symbol("}"),
      Symbol("(", "()"),
      Symbol(")"),
      Symbol("="),
      Symbol("\"", "\"\""),
      Symbol("'", "''"),
      Symbol("|"),
      Symbol("&"),
      Symbol("!"),
      Symbol("[", "[]"),
      Symbol("]"),
      Symbol("<", "<>"),
      Symbol(">"),
      Symbol("+"),
      Symbol("-"),
      Symbol("/"),
      Symbol("~"),
      Symbol("`"),
      Symbol(":"),
      Symbol("_")
    )
  }

  private class TabSymbol : Symbol("↹") {
    override val commit: String
      get() = "\t"

    override val offset: Int
      get() = 1
  }
  
  private class MoveUpSymbol : Symbol("↑") {
      override fun onCommit(editor: CodeEditor) {
          editor.moveSelection(SelectionMovement.UP)
      }

      override fun onLongCommit(editor: CodeEditor) {
          editor.moveSelection(SelectionMovement.UP)
      }
  }

  private class MoveDownSymbol : Symbol("↓") {
      override fun onCommit(editor: CodeEditor) {
          editor.moveSelection(SelectionMovement.DOWN)
      }

      override fun onLongCommit(editor: CodeEditor) {
          editor.moveSelection(SelectionMovement.DOWN)
      }
  }

  private class MoveLeftSymbol : Symbol("←") {
      override fun onCommit(editor: CodeEditor) {
          editor.moveSelection(SelectionMovement.LEFT)
      }

      override fun onLongCommit(editor: CodeEditor) {
          editor.moveSelection(SelectionMovement.LEFT)
      }
  }

  private class MoveRightSymbol : Symbol("→") {
      override fun onCommit(editor: CodeEditor) {
          editor.moveSelection(SelectionMovement.RIGHT)
      }

      override fun onLongCommit(editor: CodeEditor) {
          editor.moveSelection(SelectionMovement.RIGHT)
      }
  }
  
  private class JavaDocSymbol: Symbol("/* \n * \n * \n @author: */") {
    override val commit: String = "/*\n * \n * @author: \n */"
    
    override fun onCommit(editor: CodeEditor) {
        val cursor = editor.cursor
        val insertionIndex = cursor.left
        
        // Use low-level API to avoid buggy cursor calculation in editor.insertText
        editor.text.insert(cursor.leftLine, cursor.leftColumn, commit)
        
        // Manually calculate the new cursor position
        val cursorPositionInSnippet = commit.lastIndexOf(": ") + 2
        val finalCursorIndex = insertionIndex + cursorPositionInSnippet
        val newPos = editor.text.indexer.getCharPosition(finalCursorIndex)
        
        // Set selection to the calculated position
        editor.setSelection(newPos.line, newPos.column)
    }
  }
}