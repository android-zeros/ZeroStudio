package android.zero.studio.lsp.connection

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * A StreamConnectionProvider that connects to a local Android Unix domain socket.
 *
 * @author android_zero
 */
class LocalSocketStreamConnectionProvider(
    private val socketName: String,
    private val namespace: LocalSocketAddress.Namespace = LocalSocketAddress.Namespace.ABSTRACT
) : StreamConnectionProvider {

    private var socket: LocalSocket? = null
    private var _inputStream: InputStream? = null
    private var _outputStream: OutputStream? = null

    companion object {
        private const val TAG = "LocalSocketConn"
    }

    @Throws(IOException::class)
    override fun start() {
        try {
            Log.d(TAG, "Connecting to LocalSocket: $socketName")
            val client = LocalSocket()
            client.connect(LocalSocketAddress(socketName, namespace))
            
            socket = client
            _inputStream = client.inputStream
            _outputStream = client.outputStream
            Log.d(TAG, "Connected to LocalSocket: $socketName")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to connect to LocalSocket: $socketName", e)
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