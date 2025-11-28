package android.zero.studio.lsp

import android.content.Context
import android.zero.studio.lsp.connection.LspConnectionConfig
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.utils.FileUri
import java.io.File
import java.net.URI

/**
 * Abstract base class for all Language Server implementations.
 * Designed for high extensibility and modularity.
 *
 * @author android_zero
 */
abstract class BaseLspServer {
    /**
     * Unique identifier for this server instance (e.g., "python-lsp", "java-ls").
     * Used for caching and equality checks.
     */
    abstract val id: String

    /**
     * Human-readable name of the language (e.g., "Python", "java").
     * Displayed in UI.
     */
    abstract val languageName: String

    /**
     * Name of the server executable/implementation (e.g., "pylsp", "java-language-server").
     */
    abstract val serverName: String
    
    /**
     * List of file extensions supported by this server (without dot).
     */
    abstract val supportedExtensions: List<String>

    /**
     * Checks if the necessary binaries/files for this server are installed in the system.
     * @param context Android Context
     */
    abstract fun isInstalled(context: Context): Boolean

    /**
     * Installs the server components (extract assets, download binaries, pip install, etc.).
     * This method is expected to be blocking or long-running.
     */
    abstract fun install(context: Context)

    /**
     * Provides the connection configuration for this server.
     * This determines if we use Process, TCP, or LocalSocket.
     */
    abstract fun getConnectionConfig(context: Context): LspConnectionConfig

    /**
     * Provides initialization options to be sent to the server during the 'initialize' handshake.
     * Return null if no options are needed.
     * 
     * @param rootUri The workspace root URI.
     */
    open fun getInitializationOptions(rootUri: FileUri?): Any? = null

    /**
     * Checks if a specific file object is supported by this server.
     * Default implementation checks the file extension against [supportedExtensions].
     */
    open fun isSupported(file: File): Boolean {
        return supportedExtensions.contains(file.extension.lowercase())
    }
    
    /**
     * Creates the Sora Editor [CustomLanguageServerDefinition].
     * This bridges our abstract server configuration to the sora-editor-lsp client architecture.
     */
    fun createDefinition(context: Context): CustomLanguageServerDefinition {
        // Use the first extension as the primary key, or "unknown" fallback
        val ext = supportedExtensions.firstOrNull() ?: "unknown"
        
        return object : CustomLanguageServerDefinition(
            ext, 
            CustomLanguageServerDefinition.ServerConnectProvider {
                 // Create a new connection provider using our factory
                 getConnectionConfig(context).providerFactory().create()
            }
        ) {
             // Bridge the initialization options
             override fun getInitializationOptions(uri: URI?): Any? {
                 return this@BaseLspServer.getInitializationOptions(uri?.let { FileUri(it.path) })
             }
             
             // Override toString for debugging
             override fun toString(): String = "${this@BaseLspServer.serverName} Definition"
        }
    }
    
    override fun toString(): String = "$languageName ($serverName)"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseLspServer) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}