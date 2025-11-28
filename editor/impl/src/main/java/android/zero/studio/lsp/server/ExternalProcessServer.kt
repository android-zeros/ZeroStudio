package android.zero.studio.lsp.server

import android.content.Context
import android.zero.studio.lsp.BaseLspServer
import android.zero.studio.lsp.connection.LspConnectionConfig
import io.github.rosemoe.sora.lsp.utils.FileUri
import java.io.File
import java.net.URI
import java.util.UUID

/**
 * A generic implementation for LSP servers running as external processes (e.g., CLI tools).
 * This allows registering servers dynamically without creating subclasses.
 *
 * @param languageName Display name (e.g., "Python")
 * @param command The shell command to start the server (e.g., "pylsp")
 * @param args Arguments for the command
 * @param environment Environment variables
 * @param supportedExtensions File extensions supported (e.g., "py", "pyi")
 *
 * @author android_zero
 */
class ExternalProcessServer(
    override val languageName: String,
    private val command: String,
    private val args: List<String> = emptyList(),
    private val environment: Map<String, String> = emptyMap(),
    override val supportedExtensions: List<String>,
    private val workingDir: String? = null,
    private val initializationOptions: Any? = null
) : BaseLspServer() {

    override val id: String = "proc_${languageName}_${UUID.randomUUID()}"
    override val serverName: String = command

    override fun isInstalled(context: Context): Boolean {
        // For external processes, we assume the binary exists in PATH or absolute path.
        // A more robust check would involve `which` or `File.exists`.
        if (command.startsWith("/")) {
            return File(command).exists()
        }
        // If it's a system command, we assume true for now, or implement a specific check.
        return true
    }

    override fun install(context: Context) {
        // External servers are usually managed by package managers (Termux, pip, npm).
        // No-op for generic wrapper.
    }

    override fun getConnectionConfig(context: Context): LspConnectionConfig {
        val fullCommand = mutableListOf(command).apply { addAll(args) }
        return LspConnectionConfig.Process(
            command = fullCommand,
            environment = environment,
            workingDir = workingDir
        )
    }

    override fun getInitializationOptions(rootUri: FileUri?): Any? {
        return initializationOptions
    }

    override fun toString(): String {
        return "ExternalProcessServer($languageName, cmd='$command')"
    }
}