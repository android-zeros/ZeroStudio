package ru.zdevs.intellij.c.lang

import com.intellij.lang.Language

class ClangdLanguage: Language("clangd"){
    companion object {
        val INSTANCE = ClangdLanguage()
    }
}