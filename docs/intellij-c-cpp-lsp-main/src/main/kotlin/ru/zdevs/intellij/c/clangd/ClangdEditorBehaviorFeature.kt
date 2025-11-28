package ru.zdevs.intellij.c.clangd

import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.client.features.EditorBehaviorFeature
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures

class ClangdEditorBehaviorFeature(clientFeatures: LSPClientFeatures) : EditorBehaviorFeature(clientFeatures) {
    override fun isEnableEnterBetweenBracesFix(file: PsiFile): Boolean {
        return true
    }

    override fun isEnableTextMateNestedBracesImprovements(file: PsiFile): Boolean {
        return true
    }

    override fun isEnableStatementTerminatorImprovements(file: PsiFile): Boolean {
        return true
    }

    override fun isEnableStringLiteralImprovements(file: PsiFile): Boolean {
        return true
    }
}