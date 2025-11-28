package android.zero.studio.lsp.connection

import android.util.Log
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A StreamConnectionProvider that connects via WebSocket.
 * Bridges WebSocket messages to InputStream/OutputStream.
 *
 * @author android_zero
 */
class WebSocketStreamConnectionProvider(
    private val url: String
) : StreamConnectionProvider {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep alive
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    // Pipe WebSocket incoming messages to InputStream for LSP4J to read
    private val pipeIn = PipedInputStream(16384) // Large buffer
    private val pipeOut = PipedOutputStream()
    
    // Write to this stream to send to WebSocket
    private val wsOutputStream = WebSocketOutputStream()

    private val connectionLatch = CountDownLatch(1)
    @Volatile private var connectionError: Throwable? = null

    init {
        try {
            pipeOut.connect(pipeIn)
        } catch(e: IOException) {
            // This should not happen with a newly created PipedOutputStream
            Log.e(TAG, "Failed to connect piped streams", e)
        }
    }

    companion object {
        private const val TAG = "WebSocketConn"
    }

    @Throws(IOException::class)
    override fun start() {
        val request = Request.Builder().url(url).build()
        
        Log.d(TAG, "Connecting to WebSocket: $url")
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Opened")
                connectionLatch.countDown()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    // LSP4J expects bytes.
                    synchronized(pipeOut) {
                        pipeOut.write(text.toByteArray(Charsets.UTF_8))
                        pipeOut.flush()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error writing to pipe", e)
                    close()
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    synchronized(pipeOut) {
                        pipeOut.write(bytes.toByteArray())
                        pipeOut.flush()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error writing to pipe", e)
                    close()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: $code / $reason")
                webSocket.close(1000, null)
                connectionLatch.countDown() // Ensure not blocked
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure", t)
                connectionError = t
                connectionLatch.countDown()
                close()
            }
        })

        // Wait for connection
        try {
            if (!connectionLatch.await(10, TimeUnit.SECONDS)) {
                throw IOException("WebSocket connection timed out")
            }
            connectionError?.let { throw IOException("WebSocket connection failed", it) }
        } catch (e: InterruptedException) {
            throw IOException("WebSocket connection interrupted", e)
        }
    }

    override val inputStream: InputStream
        get() = pipeIn

    override val outputStream: OutputStream
        get() = wsOutputStream

    override fun close() {
        try {
            webSocket?.close(1000, "Client closing")
        } catch (e: Exception) { /* Ignored */ }
        
        try {
            pipeOut.close()
        } catch (e: Exception) {}
        
        try {
            pipeIn.close()
        } catch (e: Exception) {}
        
        webSocket = null
    }

    /**
     * OutputStream that writes directly to the WebSocket.
     */
    private inner class WebSocketOutputStream : OutputStream() {
        override fun write(b: Int) {
            write(byteArrayOf(b.toByte()), 0, 1)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (webSocket == null) throw IOException("WebSocket not connected")
            // Use the recommended extension function `toByteString`
            val data = b.toByteString(off, len)
            val sent = webSocket?.send(data) ?: false
            if (!sent) throw IOException("Failed to send data to WebSocket")
        }

        override fun flush() {
            // WebSocket sends immediately usually
        }

        override fun close() {
            this@WebSocketStreamConnectionProvider.close()
        }
    }
}