package ru.zdevs.intellij.c.clangd

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl

class ClangdLanguageClient(project: Project) : LanguageClientImpl(project)
