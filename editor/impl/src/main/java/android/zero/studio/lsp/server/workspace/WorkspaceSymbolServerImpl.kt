package android.zero.studio.lsp.servers.workspace

import android.util.Log
import io.github.rosemoe.sora.lsp.utils.FileUri
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.SignatureInformation
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * LSP Server implementation for providing workspace-wide symbol intelligence.
 * It delegates all logic to the WorkspaceSymbolIndexer singleton.
 *
 * @author android_zero
 */
class WorkspaceSymbolServerImpl : LanguageServer, TextDocumentService, LanguageClientAware {
    private var client: LanguageClient? = null

    companion object {
        private const val TAG = "WorkspaceSymbolServer"
    }

    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
        Log.i(TAG, "Initializing Workspace Symbol Server...")
        
        // Asynchronously index the entire workspace on initialization
        params?.rootUri?.let { uriString ->
            CompletableFuture.runAsync {
                val root = File(FileUri(uriString).path)
                if (root.isDirectory) {
                    Log.d(TAG, "Starting workspace indexing for: ${root.path}")
                    root.walkTopDown()
                        .filter { it.isFile && it.length() < 1 * 10240 * 10240 }
                        .forEach { WorkspaceSymbolIndexer.indexFile(it) }
                    Log.i(TAG, "Workspace indexing complete.")
                }
            }
        }

        val capabilities = ServerCapabilities().apply {
            setTextDocumentSync(TextDocumentSyncKind.Incremental)
            setCompletionProvider(null) // Using dynamic registration
            setDefinitionProvider(true)
            setSignatureHelpProvider(null) // Simple signatures, dynamic
        }
        return CompletableFuture.completedFuture(InitializeResult(capabilities))
    }

    override fun initialized(params: InitializedParams?) {
        Log.i(TAG, "Server initialized.")
    }

    override fun shutdown(): CompletableFuture<Any> {
        Log.i(TAG, "Shutting down...")
        return CompletableFuture.completedFuture(null)
    }

    override fun exit() {
        Log.i(TAG, "Exiting.")
    }

    override fun getTextDocumentService(): TextDocumentService = this

    override fun getWorkspaceService(): WorkspaceService? = null // Not implemented

    override fun connect(client: LanguageClient?) {
        this.client = client
    }

    // --- TextDocumentService Methods ---

    override fun didOpen(params: DidOpenTextDocumentParams?) {
        params?.textDocument?.uri?.let { uri ->
            WorkspaceSymbolIndexer.indexFile(File(FileUri(uri).path))
        }
    }

    override fun didChange(params: DidChangeTextDocumentParams?) {
        // For simplicity, we re-index on every change.
        // A more advanced implementation would apply the text edits to the index.
        params?.textDocument?.uri?.let { uri ->
            WorkspaceSymbolIndexer.indexFile(File(FileUri(uri).path))
        }
    }

    override fun didClose(params: DidCloseTextDocumentParams?) {
        params?.textDocument?.uri?.let { uri ->
            WorkspaceSymbolIndexer.removeFile(uri)
        }
    }

    override fun didSave(params: org.eclipse.lsp4j.DidSaveTextDocumentParams?) {
        // No-op, didChange is sufficient for our indexing strategy.
    }

    override fun completion(params: CompletionParams?): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        if (params == null) return CompletableFuture.completedFuture(Either.forLeft(emptyList()))

        val line = params.textDocument.uri.let { uri ->
            File(FileUri(uri).path).bufferedReader().useLines { it.elementAtOrNull(params.position.line) ?: "" }
        }
        
        // Find prefix for completion
        var prefixEnd = params.position.character
        var prefixStart = prefixEnd
        while (prefixStart > 0 && line[prefixStart - 1].isLetterOrDigit() || line.getOrNull(prefixStart-1) == '.') {
            prefixStart--
        }
        val prefix = line.substring(prefixStart, prefixEnd)
        
        val items = WorkspaceSymbolIndexer.getCompletions(prefix).map {
            CompletionItem(it).apply {
                kind = CompletionItemKind.Text
            }
        }
        
        return CompletableFuture.completedFuture(Either.forLeft(items))
    }

    override fun definition(params: DefinitionParams?): CompletableFuture<Either<List<Location>, List<org.eclipse.lsp4j.LocationLink>>> {
        if (params == null) return CompletableFuture.completedFuture(Either.forLeft(emptyList()))
        
        val line = params.textDocument.uri.let { uri ->
            File(FileUri(uri).path).bufferedReader().useLines { it.elementAtOrNull(params.position.line) ?: "" }
        }
        
        // Extract symbol under cursor
        var start = params.position.character
        while (start > 0 && (line[start - 1].isLetterOrDigit() || line.getOrNull(start-1) == '.' || line.getOrNull(start-1) == '_')) {
            start--
        }
        var end = params.position.character
        while (end < line.length && (line[end].isLetterOrDigit() || line.getOrNull(end) == '.' || line.getOrNull(end) == '_')) {
            end++
        }
        val symbol = line.substring(start, end)
        
        val location = WorkspaceSymbolIndexer.findDefinition(symbol)
        val result = location?.let { listOf(Location(it.uri, it.range)) } ?: emptyList()
        
        return CompletableFuture.completedFuture(Either.forLeft(result))
    }

    override fun signatureHelp(params: SignatureHelpParams?): CompletableFuture<SignatureHelp> {
        if (params == null) return CompletableFuture.completedFuture(SignatureHelp(emptyList(), -1, -1))
        
        val line = params.textDocument.uri.let { uri ->
            File(FileUri(uri).path).bufferedReader().useLines { it.elementAtOrNull(params.position.line) ?: "" }
        }
        
        // Simple heuristic: find function call name before the parenthesis
        var parenPos = params.position.character - 1
        while (parenPos >= 0 && line[parenPos] != '(') parenPos--
        
        var start = parenPos - 1
        while (start >= 0 && (line[start].isLetterOrDigit() || line[start] == '_')) start--
        start++
        
        val functionName = line.substring(start, parenPos)
        
        val signatures = WorkspaceSymbolIndexer.findSignatures(functionName)?.map {
            SignatureInformation(it)
        } ?: emptyList()

        return CompletableFuture.completedFuture(SignatureHelp(signatures, 0, 0))
    }
}