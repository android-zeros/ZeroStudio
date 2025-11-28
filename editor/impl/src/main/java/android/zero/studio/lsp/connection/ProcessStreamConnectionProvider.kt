package android.zero.studio.lsp.connection

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * A StreamConnectionProvider that spawns a sub-process and communicates via stdin/stdout.
 *
 * @author android_zero
 */
class ProcessStreamConnectionProvider(
    private val command: List<String>,
    private val environment: Map<String, String> = emptyMap(),
    private val workingDir: String? = null
) : StreamConnectionProvider {

    private var process: Process? = null
    private var _inputStream: InputStream? = null
    private var _outputStream: OutputStream? = null
    
    companion object {
        private const val TAG = "ProcessConn"
    }

    @Throws(IOException::class)
    override fun start() {
        if (process != null) throw IOException("Process already started")

        try {
            Log.d(TAG, "Starting process: $command")
            val builder = ProcessBuilder(command)
            
            if (environment.isNotEmpty()) {
                builder.environment().putAll(environment)
            }
            
            if (workingDir != null) {
                val dir = File(workingDir)
                if (dir.exists() && dir.isDirectory) {
                    builder.directory(dir)
                } else {
                    Log.w(TAG, "Working directory invalid: $workingDir, using default")
                }
            }

            val proc = builder.start()
            
            if (!proc.isAlive) {
                 val errorMsg = proc.errorStream.bufferedReader().readText()
                 throw IOException("Process exited immediately with code ${proc.exitValue()}: $errorMsg")
            }

            process = proc
            _inputStream = proc.inputStream
            _outputStream = proc.outputStream
            
            val pidString = getPidSafely(proc)
            Log.d(TAG, "Process started successfully, PID: $pidString")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start process", e)
            close()
            throw IOException("Failed to start process", e)
        }
    }
    
    /**
     * Safely gets the process ID across different Android API levels.
     * - On API 26+, it tries to invoke the `pid()` method via reflection.
     * - On older APIs or if the method fails, it falls back to accessing the private 'pid' field.
     * This ensures compilability with older SDKs and runtime compatibility.
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun getPidSafely(process: Process): String {
        // Strategy 1: Use the official pid() method on API 26+ via reflection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val pidMethod = Process::class.java.getMethod("pid")
                val pid = pidMethod.invoke(process) as Long
                return pid.toString()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to call pid() method via reflection, falling back to field access.", e)
            }
        }

        // Strategy 2: Fallback to accessing the 'pid' field for pre-API 26 or if method call failed
        return try {
            val processClass = process.javaClass
            // The field is typically 'pid' in java.lang.UNIXProcess on Android
            val pidField = processClass.getDeclaredField("pid")
            pidField.isAccessible = true
            pidField.getInt(process).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not reflectively access 'pid' field. This might happen on some devices.", e)
            "unknown"
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

        val proc = process
        if (proc != null) {
            try {
                proc.destroy()
                if (!proc.waitFor(500, TimeUnit.MILLISECONDS)) {
                    proc.destroyForcibly()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error killing process", e)
            }
        }
        
        _inputStream = null
        _outputStream = null
        process = null
    }
}