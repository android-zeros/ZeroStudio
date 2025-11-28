package android.zero.studio.lsp.servers.workspace

import android.content.Context
import android.zero.studio.lsp.BaseLspServer
import android.zero.studio.lsp.connection.LspConnectionConfig


/**
 * LSP Server definition for the generic Workspace Symbol provider.
 * This server provides basic completions and definitions for any file type in the workspace.
 * It connects to the [WorkspaceSymbolService] via a local socket.
 *
 * @author android_zero
 */
class WorkspaceSymbolServer : BaseLspServer() {

    override val id: String = "workspace-symbol-server"
    override val languageName: String = "Workspace Symbols"
    override val serverName: String = "AndroidIDE Workspace Indexer"
    
    /**
     * This server supports all text-based files.
     * We use a wildcard to signify it's a fallback/general purpose server.
     */
    override val supportedExtensions: List<String> = listOf("*")

    /**
     * The service is part of the app, so it's always "installed".
     */
    override fun isInstalled(context: Context): Boolean = true

    /**
     * No installation needed as it's an internal service.
     */
    override fun install(context: Context) {
        // No-op
    }

    /**
     * Specifies the connection via LocalSocket, matching the name in [WorkspaceSymbolService].
     */
    override fun getConnectionConfig(context: Context): LspConnectionConfig {
        return LspConnectionConfig.LocalSocket(WorkspaceSymbolService.SOCKET_NAME)
    }
    
    /**
     * We override isSupported to always return true, making it a universal fallback provider.
     * The LspManager should have logic to prioritize specific language servers over this one.
     */
    override fun isSupported(file: java.io.File): Boolean {
        // This server is a fallback for any file that doesn't have a specific server.
        // The registry should handle priority. For now, we say yes to everything.
        return true
    }
}