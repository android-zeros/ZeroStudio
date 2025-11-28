package com.itsaky.androidide.search

import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import com.blankj.utilcode.util.FileUtils
import com.itsaky.androidide.models.Position
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.projects.IProjectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.regex.Pattern
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

/**
 * 项目搜索引擎
 * @author android_zero
 */
object ProjectSearchEngine {

    private const val MAX_PREVIEW_LENGTH = 120
    private const val COLOR_HIGHLIGHT_BG = 0xFF007ACC.toInt() // Android Studio 风格高亮
    private const val COLOR_HIGHLIGHT_FG = 0xFFFFFFFF.toInt()

    /**
     * 执行搜索，返回结果流
     */
    fun search(config: SearchConfig): Flow<List<SearchResultItem>> = flow {
        val pattern = config.getPattern() ?: return@flow
        val projectManager = IProjectManager.getInstance()
        val workspace = projectManager.getWorkspace() ?: return@flow

        // 获取待搜索的文件列表 (基于索引和 Scope)
        val filesToSearch = getFilesForScope(config, workspace)
        
        val buffer = ArrayList<SearchResultItem>()
        val batchSize = 20 // 批处理大小，避免 UI 刷新过快

        for (file in filesToSearch) {
            if (!coroutineContext.isActive) break
            
            // 检查排除规则
            if (isExcluded(file, config)) continue
            // 检查文件掩码
            if (!matchesMask(file, config.fileMasks)) continue

            if (config.scope == SearchScope.FILE) {
                // 纯文件名搜索
                if (pattern.matcher(file.name).find()) {
                    buffer.add(FileHeaderResult(file, 0, workspace.findModuleForFile(file)?.name))
                }
            } else {
                // 文本内容搜索
                val matches = searchFileContent(file, pattern, config)
                if (matches.isNotEmpty()) {
                    val moduleName = workspace.findModuleForFile(file)?.name
                    buffer.add(FileHeaderResult(file, matches.size, moduleName))
                    buffer.addAll(matches)
                }
            }

            if (buffer.size >= batchSize) {
                emit(ArrayList(buffer))
                buffer.clear()
            }
        }

        if (buffer.isNotEmpty()) {
            emit(buffer)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 根据 Scope 获取文件列表，优先使用 IDE 索引
     */
    private fun getFilesForScope(config: SearchConfig, workspace: com.itsaky.androidide.projects.IWorkspace): Sequence<File> {
        return when (config.scope) {
            SearchScope.CURRENT_FILE -> sequenceOf(config.currentFile).filterNotNull()
            SearchScope.DIRECTORY -> {
                config.targetDirectory?.walk()?.filter { it.isFile } ?: emptySequence()
            }
            SearchScope.MODULE -> {
                config.targetModule?.walk()?.filter { it.isFile } ?: emptySequence()
            }
            SearchScope.ALL, SearchScope.SCOPE_CUSTOM -> {
                // 利用 ProjectManager 的索引获取所有 Source Files，比 IO 遍历快
                val allSources = workspace.getSubProjects()
                    .flatMap { 
                        if(it is com.itsaky.androidide.projects.ModuleProject) it.getSourceDirectories() else emptySet() 
                    }
                    .flatMap { it.walk() } // 遍历 Source 目录
                    .filter { it.isFile }
                
                // 如果是 ALL，还需要加上非源码文件 (例如 build.gradle, assets)
                if (config.scope == SearchScope.ALL) {
                   workspace.getProjectDir().walk().filter { it.isFile }
                } else {
                   allSources.asSequence()
                }
            }
            else -> emptySequence()
        }
    }

    // 修复点：将返回类型和列表泛型修正为 TextMatchResult
    private fun searchFileContent(file: File, pattern: Pattern, config: SearchConfig): List<TextMatchResult> {
        val results = ArrayList<TextMatchResult>()
        if (!FileUtils.isUtf8(file)) return results // 跳过二进制文件

        try {
            BufferedReader(FileReader(file)).use { reader ->
                var lineIndex = 0
                var line: String? = reader.readLine()
                
                // 拥挤模式检测：如果单个文件结果太多，后续开启拥挤模式
                var matchCountInFile = 0
                val crowdThreshold = 5

                while (line != null) {
                    val matcher = pattern.matcher(line)
                    while (matcher.find()) {
                        matchCountInFile++
                        val isCrowded = matchCountInFile > crowdThreshold
                        
                        val start = matcher.start()
                        val end = matcher.end()
                        val range = Range(Position(lineIndex, start), Position(lineIndex, end))
                        
                        val preview = createPreview(line!!, start, end)
                        
                        results.add(TextMatchResult(
                            file,
                            lineIndex,
                            line!!,
                            range,
                            preview,
                            isCrowded
                        ))
                    }
                    
                    line = reader.readLine()
                    lineIndex++
                }
            }
        } catch (e: Exception) {
            // 忽略读取错误
        }
        return results
    }

    /**
     * 创建带高亮的文本快照，智能截取过长的行
     */
    private fun createPreview(line: String, start: Int, end: Int): CharSequence {
        val trimStart = max(0, start - 40)
        val trimEnd = min(line.length, end + 40)
        
        var text = line.substring(trimStart, trimEnd)
        if (trimStart > 0) text = "...$text"
        if (trimEnd < line.length) text = "$text..."

        val span = SpannableString(text)
        
        // 计算在截取后文本中的相对位置
        // 修正高亮位置计算逻辑
        val prefixLen = if (trimStart > 0) 3 else 0 // "..." 的长度
        val highlightStart = (start - trimStart) + prefixLen
        val highlightEnd = highlightStart + (end - start)
        
        if (highlightStart >= 0 && highlightEnd <= span.length) {
            span.setSpan(BackgroundColorSpan(COLOR_HIGHLIGHT_BG), highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(ForegroundColorSpan(COLOR_HIGHLIGHT_FG), highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        return span
    }

    private fun isExcluded(file: File, config: SearchConfig): Boolean {
        // 默认排除
        if (file.name.startsWith(".") || file.absolutePath.contains("/build/")) return true
        // 用户自定义排除
        return config.excludePatterns.any { file.absolutePath.contains(it) }
    }

    private fun matchesMask(file: File, masks: List<String>): Boolean {
        if (masks.isEmpty() || masks.contains("ALL")) return true
        val name = file.name
        return masks.any { mask ->
            if (mask.startsWith("*.")) {
                name.endsWith(mask.substring(1))
            } else {
                name == mask
            }
        }
    }
}