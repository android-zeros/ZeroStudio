package com.itsaky.androidide.editor.language

import com.itsaky.androidide.editor.language.treesitter.TSLanguageRegistry
import com.itsaky.androidide.editor.language.treesitter.TreeSitterLanguage
import com.itsaky.androidide.editor.ui.IDEEditor
import com.itsaky.androidide.treesitter.TSLanguage
import com.itsaky.androidide.treesitter.TSTree
import io.github.rosemoe.sora.editor.ts.TsAnalyzeManager
import io.github.rosemoe.sora.text.Content
import android.os.Bundle

/**
 * 语言分析工具
 */
object LanguageAnalysisBridge {

    /**
     * 获取编辑器当前语法
     */
    fun getSyntaxTree(editor: IDEEditor): TSTree? {
        return try {
            val lang = editor.editorLanguage as? TreeSitterLanguage ?: return null
            val analyzeManager = lang.getAnalyzeManager() as? TsAnalyzeManager ?: return null
            resetAnalyzeManagerWithoutReference(analyzeManager, editor.text)
            getTreeFromPublicMethod(analyzeManager)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取编辑器TSLanguage
     */
    fun getTsLanguage(editor: IDEEditor): TSLanguage? {
        return try {
            val file = editor.file ?: return null
            val fileType = file.extension
            getLanguage(fileType, editor)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 根据文件类型获取TSLanguage
     */
    fun getLanguage(fileType: String, editor: IDEEditor): TSLanguage? {
        return try {
            val registry = TSLanguageRegistry.instance
            if (!registry.hasLanguage(fileType)) {
                return null
            }
            val factory = registry.getFactory<TreeSitterLanguage>(fileType)
            val lang = factory.create(editor.context)
            getTsLanguageFromPublicMethod(lang)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 重置分析管理
     */
    private fun resetAnalyzeManagerWithoutReference(manager: TsAnalyzeManager, content: Content) {
        try {
            val method = manager.javaClass.getMethod(
                "reset", 
                Content::class.java, 
                Bundle::class.java
            )
            method.invoke(manager, content, Bundle.EMPTY)
        } catch (e: NoSuchMethodException) {
            val referenceClass = Class.forName("io.github.rosemoe.sora.text.Content\$Reference")
            val reference = referenceClass.getConstructor(Content::class.java).newInstance(content)
            val method = manager.javaClass.getMethod(
                "reset", 
                referenceClass, 
                Bundle::class.java
            )
            method.invoke(manager, reference, Bundle.EMPTY)
        }
    }

    /**
     * 从TsAnalyzeManager公开方法获取语法树
     */
    private fun getTreeFromPublicMethod(manager: TsAnalyzeManager): TSTree? {
        return try {
            val method = manager.javaClass.getMethod("getTree")
            method.invoke(manager) as? TSTree
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    /**
     * 从TreeSitterLanguage公开方法获取TSLanguage
     */
    private fun getTsLanguageFromPublicMethod(lang: TreeSitterLanguage): TSLanguage? {
        return try {
            val method = lang.javaClass.getMethod("getLanguage")
            method.invoke(lang) as? TSLanguage
        } catch (e: NoSuchMethodException) {
            null
        }
    }
}
