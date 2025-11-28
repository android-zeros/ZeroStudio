package android.zero.studio.lsp.servers.workspace

import io.github.rosemoe.sora.lsp.utils.FileUri
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import io.github.rosemoe.sora.lsp.utils.toFileUri

/**
 * The core indexer for workspace symbols. Scans files and maintains an in-memory database
 * of words, token sequences, and their locations. This class is thread-safe.
 *
 * @author android_zero
 */
object WorkspaceSymbolIndexer {
    private val WORD_SEQUENCE_REGEX = "([a-zA-Z_][\\w.]*)".toRegex()
    private val FUNCTION_SIGNATURE_REGEX = "(\\w+)\\s*\\([^)]*\\)".toRegex()

    // Index data structures
    private val simpleWords = ConcurrentHashMap.newKeySet<String>()
    private val tokenSequences = ConcurrentHashMap.newKeySet<String>()
    private val definitions = ConcurrentHashMap<String, SymbolLocation>()
    private val signatures = ConcurrentHashMap<String, MutableSet<String>>()

    // File-to-token mapping for efficient updates
    private val fileTokens = ConcurrentHashMap<String, Set<String>>()

    data class SymbolLocation(val uri: String, val range: Range)

    /**
     * Indexes a single file. This operation is idempotent.
     * It first removes old data for the file, then adds new data.
     */
    fun indexFile(file: File) {
        if (!file.isFile || !file.canRead()) return
        val uri = FileUri(file.absolutePath).toFileUri()
        removeFile(uri)

        val newTokens = ConcurrentHashMap.newKeySet<String>()
        val content = try {
            file.readText()
        } catch (e: Exception) {
            return // Skip unreadable files
        }

        // Index word sequences (e.g., "command", "command.title")
        WORD_SEQUENCE_REGEX.findAll(content).forEach { match ->
            val sequence = match.value
            newTokens.add(sequence)
            tokenSequences.add(sequence)

            // Store simple words for basic completion
            sequence.split('.').forEach { simpleWords.add(it) }

            // Heuristic for definition: first occurrence is the definition
            if (!definitions.containsKey(sequence)) {
                val startOffset = match.range.first
                val (line, col) = offsetToPosition(content, startOffset)
                val endOffset = match.range.last + 1
                val (endLine, endCol) = offsetToPosition(content, endOffset)
                definitions.putIfAbsent(
                    sequence,
                    SymbolLocation(uri, Range(Position(line, col), Position(endLine, endCol)))
                )
            }
        }

        // Heuristic for signature help
        FUNCTION_SIGNATURE_REGEX.findAll(content).forEach { match ->
            val functionName = match.groupValues[1]
            signatures.computeIfAbsent(functionName) { ConcurrentHashMap.newKeySet() }.add(match.value)
        }

        fileTokens[uri] = newTokens
    }

    /**
     * Removes all indexed symbols from a given file.
     */
    fun removeFile(uri: String) {
        fileTokens.remove(uri)?.forEach { token ->
            // This is a slow "garbage collection". For high performance, we might just mark tokens as stale.
            // But for simplicity, we remove them.
            simpleWords.remove(token)
            tokenSequences.remove(token)
            definitions.remove(token)
            // Note: Signatures are not removed to keep them available, but this could be refined.
        }
    }

    fun getCompletions(prefix: String): List<String> {
        val result = mutableSetOf<String>()
        val targetSet = if (prefix.contains('.')) tokenSequences else simpleWords
        
        targetSet.forEach { token ->
            if (token.startsWith(prefix, ignoreCase = true)) {
                result.add(token)
            }
        }
        return result.toList().sorted()
    }

    fun findDefinition(symbol: String): SymbolLocation? {
        return definitions[symbol]
    }

    fun findSignatures(functionName: String): Set<String>? {
        return signatures[functionName]
    }

    private fun offsetToPosition(text: String, offset: Int): Pair<Int, Int> {
        val lines = text.substring(0, offset).lines()
        val line = lines.size - 1
        val column = lines.last().length
        return line to column
    }
}