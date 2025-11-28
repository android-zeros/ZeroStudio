package android.zero.studio.lsp.connection

import android.util.Log
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * A StreamConnectionProvider that connects to a TCP socket.
 *
 * @author android_zero
 */
class SocketStreamConnectionProvider(
    private val host: String,
    private val port: Int,
    private val timeoutMs: Int = 5000
) : StreamConnectionProvider {

    private var socket: Socket? = null
    private var _inputStream: InputStream? = null
    private var _outputStream: OutputStream? = null

    companion object {
        private const val TAG = "TcpSocketConn"
    }

    @Throws(IOException::class)
    override fun start() {
        try {
            Log.d(TAG, "Connecting to TCP Socket: $host:$port")
            val client = Socket()
            client.connect(InetSocketAddress(host, port), timeoutMs)
            // Disable Nagle's algorithm for lower latency (important for LSP interactive traffic)
            client.tcpNoDelay = true
            
            socket = client
            _inputStream = client.inputStream
            _outputStream = client.outputStream
            Log.d(TAG, "Connected to TCP Socket: $host:$port")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to connect to TCP Socket: $host:$port", e)
            close()
            throw e
        }
    }

    override val inputStream: InputStream
        get() = _inputStream ?: throw IOException("Connection not started")

    override val outputStream: OutputStream
        get() = _outputStream ?: throw IOException("Connection not started")

    override fun close() {
        try {
            _inputStream?.close()
        } catch (e: Exception) { /* Ignored */ }
        
        try {
            _outputStream?.close()
        } catch (e: Exception) { /* Ignored */ }

        try {
            socket?.close()
        } catch (e: Exception) { /* Ignored */ }
        
        _inputStream = null
        _outputStream = null
        socket = null
    }
}