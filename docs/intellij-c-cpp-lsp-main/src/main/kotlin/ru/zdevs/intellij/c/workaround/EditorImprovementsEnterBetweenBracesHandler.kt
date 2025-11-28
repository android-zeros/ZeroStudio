package ru.zdevs.intellij.c.workaround

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.enter.EnterBetweenBracesFinalHandler
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.redhat.devtools.lsp4ij.LSPIJEditorUtils
import com.redhat.devtools.lsp4ij.LSPIJUtils
import ru.zdevs.intellij.c.lang.ClangdLanguage

/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 */

/**
 * This provides a workaround for [IJPL-159454](https://youtrack.jetbrains.com/issue/IJPL-159454).
 */
class EditorImprovementsEnterBetweenBracesHandler : EnterBetweenBracesFinalHandler() {
    private var enterAfterOpenBrace = false
    private var openBraceCharacter: Char? = null
    private var enterBeforeCloseBrace = false
    private var closeBraceCharacter: Char? = null

    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result {
        if (CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER &&
            LSPIJUtils.getFileLanguage(file) == ClangdLanguage.INSTANCE
        ) {
            val caretModel = editor.caretModel
            val offset = caretModel.offset
            if (offset > 0) {
                val document = editor.document
                val charsSequence = document.charsSequence
                val previousCharacter = charsSequence[offset - 1]
                if (LSPIJEditorUtils.isOpenBraceCharacter(file, previousCharacter)) {
                    enterAfterOpenBrace = true
                    openBraceCharacter = previousCharacter
                    closeBraceCharacter = LSPIJEditorUtils.getCloseBraceCharacter(file, openBraceCharacter)
                    if (closeBraceCharacter != null) {
                        val nextCharacter = charsSequence[offset]
                        if (closeBraceCharacter == nextCharacter) {
                            enterBeforeCloseBrace = true
                        }
                    }
                }
            }
        }

        return super.preprocessEnter(file, editor, caretOffset, caretAdvance, dataContext, originalHandler)
    }

    override fun postProcessEnter(
        file: PsiFile,
        editor: Editor,
        dataContext: DataContext
    ): EnterHandlerDelegate.Result {
        if (CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER &&
            LSPIJUtils.getFileLanguage(file) == ClangdLanguage.INSTANCE
        ) {
            val project = file.project
            val document = editor.document
            PsiDocumentManager.getInstance(project).commitDocument(document)

            val caretModel = editor.caretModel
            val offset = caretModel.offset
            val lineNumber = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            var lineEndOffset = document.getLineEndOffset(lineNumber)

            val documentChars = document.charsSequence

            val currentIndentSize = getCurrentIndentSize(file, document, offset)
            val indentSize = getIndentSize(file)
            val newIndentSize = currentIndentSize + indentSize
            val indentChar = if (useTab(file)) '\t' else ' '

            if (enterAfterOpenBrace && enterBeforeCloseBrace) {
                val indentedBracedPair = """
                    ${openBraceCharacter.toString()}
                    ${StringUtil.repeatSymbol(indentChar, newIndentSize)}
                    ${
                    StringUtil.repeatSymbol(
                        indentChar,
                        currentIndentSize
                    )
                }$closeBraceCharacter
                    """.trimIndent()

                val bracedPairStartOffset = StringUtil.lastIndexOf(
                    documentChars,
                    openBraceCharacter!!, 0, offset + 1
                )
                var bracedPairEndOffset = StringUtil.indexOf(
                    documentChars,
                    closeBraceCharacter!!, offset
                )
                if ((bracedPairStartOffset > -1) && (bracedPairEndOffset > -1)) {
                    // This is a workaround for a bizarre behavior where additional whitespace is being added
                    val bracedPairEndLineNumber = document.getLineNumber(bracedPairEndOffset)
                    val bracedPairEndLineEndOffset = document.getLineEndOffset(bracedPairEndLineNumber)
                    if (bracedPairEndLineEndOffset > bracedPairEndOffset) {
                        val afterBracedPairEndChars =
                            documentChars.subSequence(bracedPairEndOffset + 1, bracedPairEndLineEndOffset)
                        val afterBracedPairEndText = afterBracedPairEndChars.toString()
                        if (afterBracedPairEndText.isNotEmpty() && afterBracedPairEndText.trim { it <= ' ' }.isEmpty()) {
                            bracedPairEndOffset = bracedPairEndLineEndOffset - 1
                        }
                    }

                    document.replaceString(bracedPairStartOffset, bracedPairEndOffset + 1, indentedBracedPair)
                    caretModel.moveToOffset(lineStartOffset + newIndentSize)
                }
            } else {
                val newLineIndentSize = if (enterAfterOpenBrace) newIndentSize else currentIndentSize
                var indentedNewline = StringUtil.repeatSymbol(indentChar, newLineIndentSize)

                // Find the first non-whitespace character in the line as we'll only replace up to that point
                val lineChars = documentChars.subSequence(lineStartOffset, lineEndOffset)
                val lineText = lineChars.toString()
                val firstNonWhitespaceCharacterIndex = StringUtil.findFirst(
                    lineText
                ) { lineCharacter: Char -> !Character.isWhitespace(lineCharacter) }
                if (firstNonWhitespaceCharacterIndex > -1) {
                    lineEndOffset = firstNonWhitespaceCharacterIndex - 1
                    indentedNewline = ""
                }

                if (lineEndOffset > lineStartOffset) {
                    document.replaceString(lineStartOffset, lineEndOffset - 1, indentedNewline)
                } else {
                    document.insertString(lineStartOffset, indentedNewline)
                }
                caretModel.moveToOffset(lineStartOffset + newLineIndentSize)
            }

            enterAfterOpenBrace = false
            enterBeforeCloseBrace = false
            openBraceCharacter = null
            closeBraceCharacter = null

            return EnterHandlerDelegate.Result.Stop
        }

        return super.postProcessEnter(file, editor, dataContext)
    }

    companion object {
        private fun getCurrentIndentSize(
            file: PsiFile,
            document: Document,
            offset: Int
        ): Int {
            var currentIndentSize = 0

            val lineNumber = document.getLineNumber(offset)
            val beforeLineNumber = lineNumber - 1
            val beforeLineStartOffset = document.getLineStartOffset(beforeLineNumber)
            val beforeLineEndOffset = document.getLineEndOffset(beforeLineNumber)
            val documentChars = document.charsSequence
            val beforeLineChars = documentChars.subSequence(beforeLineStartOffset, beforeLineEndOffset)
            val beforeLineText = beforeLineChars.toString()
            if (StringUtil.isNotEmpty(beforeLineText.trim { it <= ' ' })) {
                var i = 0
                val lineLength = beforeLineText.length
                while (i < lineLength) {
                    if (!Character.isWhitespace(beforeLineText[i])) {
                        currentIndentSize = i
                        break
                    }
                    i++
                }
            } else if (beforeLineText.isNotEmpty()) {
                currentIndentSize = StringUtil.countChars(beforeLineText, if (useTab(file)) '\t' else ' ')
            }

            return currentIndentSize
        }

        private fun getCodeStyleSettings(file: PsiFile): CodeStyleSettings {
            var codeStyleSettings = CodeStyle.getSettings(file)
            if (codeStyleSettings == null) {
                codeStyleSettings = CodeStyle.getDefaultSettings()
            }
            return codeStyleSettings
        }

        private fun useTab(file: PsiFile): Boolean {
            return getCodeStyleSettings(file).useTabCharacter(file.fileType)
        }

        private fun getIndentSize(file: PsiFile): Int {
            return if (useTab(file)) 1 else getCodeStyleSettings(file).getIndentSize(file.fileType)
        }
    }
}
