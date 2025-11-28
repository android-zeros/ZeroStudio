package android.zero.studio.lsp.server

import android.content.Context
import android.zero.studio.lsp.BaseLspServer
import android.zero.studio.lsp.connection.LspConnectionConfig
import io.github.rosemoe.sora.lsp.utils.FileUri
import java.util.UUID

/**
 * A generic implementation for LSP servers running on a TCP socket.
 * Useful for connecting to servers running in Termux, remote machines, or via adb forwarding.
 *
 * @author android_zero
 */
class ExternalSocketServer(
    override val languageName: String,
    private val host: String = "127.0.0.1",
    private val port: Int,
    override val supportedExtensions: List<String>,
    private val initializationOptions: Any? = null
) : BaseLspServer() {

    override val id: String = "tcp_${languageName}_${host}_${port}"
    override val serverName: String = "$host:$port"

    override fun isInstalled(context: Context): Boolean {
        // Socket servers are "installed" if we define them. 
        // Connectivity check happens at connection time.
        return true
    }

    override fun install(context: Context) {
        // No-op
    }

    override fun getConnectionConfig(context: Context): LspConnectionConfig {
        return LspConnectionConfig.TcpSocket(host, port)
    }

    override fun getInitializationOptions(rootUri: FileUri?): Any? {
        return initializationOptions
    }

    override fun toString(): String {
        return "ExternalSocketServer($languageName, $host:$port)"
    }
}