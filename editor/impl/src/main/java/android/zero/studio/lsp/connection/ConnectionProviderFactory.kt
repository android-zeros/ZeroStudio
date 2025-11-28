package android.zero.studio.lsp.connection

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import java.io.InputStream
import java.io.OutputStream

/**
 * Factory interface for creating connection providers.
 * This allows lazy instantiation of connections.
 *
 * @author android_zero
 */
fun interface ConnectionProviderFactory {
    fun create(): StreamConnectionProvider
}

/**
 * Configuration interface for LSP connections.
 * Implementations define how to connect to a language server.
 *
 * @author android_zero
 */
sealed interface LspConnectionConfig {
    /**
     * Creates a factory that produces the actual StreamConnectionProvider.
     */
    fun providerFactory(): ConnectionProviderFactory

    /**
     * Connects via a local Unix domain socket.
     * Suitable for Android Services communicating via LocalSocket.
     */
    data class LocalSocket(
        val socketName: String
    ) : LspConnectionConfig {
        override fun providerFactory() = ConnectionProviderFactory { 
            LocalSocketStreamConnectionProvider(socketName) 
        }
    }

    /**
     * Connects via a TCP socket.
     * Suitable for remote servers or local servers listening on a port.
     */
    data class TcpSocket(
        val host: String = "127.0.0.1",
        val port: Int
    ) : LspConnectionConfig {
        override fun providerFactory() = ConnectionProviderFactory { 
            SocketStreamConnectionProvider(host, port) 
        }
    }

    /**
     * Connects by spawning a sub-process.
     * Suitable for language servers that run as standalone binaries (e.g., clangd, pylsp).
     * 
     * @param command The command line arguments to start the process.
     * @param environment Custom environment variables.
     * @param workingDir The working directory for the process.
     */
    data class Process(
        val command: List<String>,
        val environment: Map<String, String> = emptyMap(),
        val workingDir: String? = null
    ) : LspConnectionConfig {
        override fun providerFactory() = ConnectionProviderFactory { 
            ProcessStreamConnectionProvider(command, environment, workingDir) 
        }
    }
    
    /**
     * Connects via WebSocket (Client Mode).
     * Suitable for cloud-based language servers.
     */
    data class WebSocket(
        val url: String
    ) : LspConnectionConfig {
        override fun providerFactory() = ConnectionProviderFactory {
            WebSocketStreamConnectionProvider(url)
        }
    }
}