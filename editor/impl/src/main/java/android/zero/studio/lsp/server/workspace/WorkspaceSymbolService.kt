package android.zero.studio.lsp.servers.workspace

import android.app.Service
import android.content.Intent
import android.net.LocalServerSocket
import android.os.IBinder
import android.util.Log
import android.zero.studio.lsp.servers.workspace.WorkspaceSymbolServerImpl
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.thread

/**
 * Android Service to host the Workspace Symbol Language Server.
 *
 * @author android_zero
 */
class WorkspaceSymbolService : Service() {

    private var serverFuture: Future<Void>? = null
    private lateinit var serverSocket: LocalServerSocket
    private lateinit var executor: ExecutorService

    companion object {
        private const val TAG = "WorkspaceSymbolService"
        const val SOCKET_NAME = "android-ide-workspace-symbol-lsp"
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Starting Workspace Symbol Service.")
        
        executor = Executors.newCachedThreadPool()
        
        thread(start = true, isDaemon = true, name = "WorkspaceSymbolServerThread") {
            try {
                serverSocket = LocalServerSocket(SOCKET_NAME)
                Log.i(TAG, "Workspace symbol socket opened at: ${serverSocket.localSocketAddress}")

                while (!Thread.currentThread().isInterrupted) {
                    val clientSocket = serverSocket.accept()
                    Log.i(TAG, "Workspace symbol client connected.")

                    try {
                        val server = WorkspaceSymbolServerImpl()
                        val launcher = Launcher.createLauncher(
                            server,
                            LanguageClient::class.java,
                            clientSocket.inputStream,
                            clientSocket.outputStream,
                            executor,
                            { writer -> writer }
                        )
                        
                        server.connect(launcher.remoteProxy)
                        serverFuture = launcher.startListening()
                        serverFuture?.get() // Block for this connection
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during workspace symbol LSP session.", e)
                    } finally {
                        clientSocket.close()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Workspace symbol socket failed or closed.", e)
            } finally {
                shutdownServer()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroying Workspace Symbol Service.")
        shutdownServer()
        super.onDestroy()
    }

    private fun shutdownServer() {
        serverFuture?.cancel(true)
        if (::serverSocket.isInitialized) {
            try {
                serverSocket.close()
            } catch (e: IOException) { /* Ignored */ }
        }
        if (::executor.isInitialized && !executor.isShutdown) {
            executor.shutdownNow()
        }
    }
}