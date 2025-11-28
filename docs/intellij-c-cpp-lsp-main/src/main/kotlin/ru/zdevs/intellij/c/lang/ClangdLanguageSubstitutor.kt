package ru.zdevs.intellij.c.lang

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.LanguageSubstitutor

class ClangdLanguageSubstitutor : LanguageSubstitutor() {
    override fun getLanguage(virtualFile: VirtualFile, project: Project): Language? {
        return Language.findLanguageByID("textmate")
    }
}