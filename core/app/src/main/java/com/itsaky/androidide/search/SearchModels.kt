package com.itsaky.androidide.search

import com.itsaky.androidide.models.Range
import java.io.File
import java.util.regex.Pattern

/**
 * 搜索范围定义
 */
enum class SearchScope {
    ALL,                // 整个项目 (Project + Libraries)
    MODULE,             // 指定模块 (需要动态获取模块列表)
    FILE,               // 纯文件搜索 (不搜索内容)
    DIRECTORY,          // 指定目录
    SCOPE_CUSTOM,       // 自定义范围 (Project Source Files, etc.)
    CURRENT_FILE        // 当前编辑器文件
}

/**
 * 自定义 Scope 类型 (对应 Android Studio 下拉菜单)
 */
enum class CustomScopeType(val label: String) {
    ALL_PLACES("All Places"),
    PROJECT_FILES("Project Files"),
    PROJECT_AND_LIBRARIES("Project and Libraries"),
    PROJECT_SOURCE_FILES("Project Source Files"),
    OPEN_FILES("Open Files")
}

/**
 * 搜索配置参数 (Immutable)
 */
data class SearchConfig(
    val query: String,
    val replacement: String? = null,
    val scope: SearchScope,
    val customScope: CustomScopeType = CustomScopeType.ALL_PLACES,
    val targetModule: File? = null, // 模块根目录
    val targetDirectory: File? = null, // 指定搜索目录
    val currentFile: File? = null, // 当前编辑的文件
    val fileMasks: List<String> = emptyList(), // e.g. [".java", ".xml"]
    val isCaseSensitive: Boolean = false,
    val isWholeWord: Boolean = false,
    val isRegex: Boolean = false,
    val excludePatterns: List<String> = emptyList() // 排除规则
) {
    // 预编译正则，提高性能
    fun getPattern(): Pattern? {
        if (query.isEmpty()) return null
        var flags = if (!isCaseSensitive) Pattern.CASE_INSENSITIVE else 0
        flags = flags or Pattern.MULTILINE
        
        var regex = if (isRegex) query else Pattern.quote(query)
        if (isWholeWord) {
            regex = "\\b$regex\\b"
        }
        return try {
            Pattern.compile(regex, flags)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 搜索结果基类
 */
sealed class SearchResultItem(open val file: File)

/**
 * 文件头结果 (显示文件路径，作为分组头)
 */
data class FileHeaderResult(
    override val file: File,
    val matchCount: Int,
    val moduleName: String? // 显示所属模块
) : SearchResultItem(file)

/**
 * 文本内容匹配结果 (快照)
 */
data class TextMatchResult(
    override val file: File,
    val lineIndex: Int, // 0-based
    val lineContent: String, // 原始行文本
    val matchRange: Range, // 匹配的高亮范围
    val previewText: CharSequence, // 处理过的带高亮 Span 的文本 (用于 UI 显示)
    val isCrowded: Boolean = false // 是否拥挤模式 (只显示极简信息)
) : SearchResultItem(file)