package ru.zdevs.intellij.c.clangd

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.client.features.LSPSelectionRangeFeature
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider


class ClangdLanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return ClangdLanguageServer(project)
    }

    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return ClangdLanguageClient(project)
    }

    override fun createClientFeatures(): LSPClientFeatures {
        val feature = LSPClientFeatures()
        feature.selectionRangeFeature = DisabledLSPSelectionRangeFeature()
        return feature.setEditorBehaviorFeature(ClangdEditorBehaviorFeature(feature))
    }

    class DisabledLSPSelectionRangeFeature : LSPSelectionRangeFeature() {
        override fun isSupported(file: PsiFile): Boolean {
            return false
        }
    }
}
