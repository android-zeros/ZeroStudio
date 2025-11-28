package com.itsaky.androidide.services.sdk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.itsaky.androidide.R
import com.itsaky.androidide.activities.SdkManagerActivity
import com.itsaky.androidide.utils.Environment
import kotlinx.coroutines.*
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * A foreground service to handle downloading and installing SDK components in the background.
 * It communicates its state (progress, completion, failure) back to the UI
 * using LocalBroadcastManager.
 *
 * @author Your Name / Akash Yadav
 */
class SdkManagerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadUrl = intent?.getStringExtra(EXTRA_URL)
        val destinationSubPath = intent?.getStringExtra(EXTRA_DEST_SUBPATH)
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME)

        if (downloadUrl == null || destinationSubPath == null || packageName == null) {
            // Invalid intent, stop the service.
            stopSelf()
            return START_NOT_STICKY
        }
        
        val notification = createNotification(packageName, "准备下载...", 0)
        startForeground(NOTIFICATION_ID, notification)

        scope.launch {
            try {
                broadcastUpdate(downloadUrl, STATUS_STARTED)
                downloadAndInstall(downloadUrl, destinationSubPath, packageName)
                broadcastUpdate(downloadUrl, STATUS_COMPLETED)
            } catch (e: Exception) {
                e.printStackTrace()
                broadcastUpdate(downloadUrl, STATUS_FAILED, error = e.message)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelfResult(startId)
            }
        }

        return START_STICKY
    }

    private suspend fun downloadAndInstall(url: String, destPath: String, packageName: String) {
        val tempFile = File(cacheDir, "sdk_download_${System.currentTimeMillis()}.tmp")
        
        // Handle both relative (to ANDROID_HOME) and absolute destination paths.
        val destination = if (destPath.startsWith(File.separator)) {
            File(destPath)
        } else if (destPath == ".") {
            Environment.ANDROID_HOME
        } else {
            File(Environment.ANDROID_HOME, destPath)
        }

        // --- Download Step ---
        withContext(Dispatchers.IO) {
            URL(url).openStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val totalBytes = try { URL(url).openConnection().contentLengthLong } catch (e: Exception) { -1L }
                    var bytesCopied = 0L
                    val buffer = ByteArray(8 * 1024)
                    var bytes = input.read(buffer)
                    while (bytes >= 0 && isActive) { // Check if coroutine is cancelled
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        if (totalBytes > 0) {
                            val progress = ((bytesCopied * 100) / totalBytes).toInt()
                            updateNotification(packageName, "下载中...", progress)
                            broadcastUpdate(url, STATUS_PROGRESS, progress)
                        }
                        bytes = input.read(buffer)
                    }
                }
            }
        }
        
        if (!scope.isActive) { // Check for cancellation after download
            tempFile.delete()
            return
        }

        // --- Installation (Extraction) Step ---
        updateNotification(packageName, "安装中...", -1) // Indeterminate progress
        broadcastUpdate(url, STATUS_EXTRACTING)

        withContext(Dispatchers.IO) {
            destination.mkdirs()
            XZInputStream(tempFile.inputStream().buffered()).use { xzIn ->
                TarArchiveInputStream(xzIn).use { tarIn ->
                    var entry = tarIn.nextTarEntry
                    while (entry != null && isActive) { // Check for cancellation
                        val destFile = File(destination, entry.name)
                        if (entry.isDirectory) {
                            destFile.mkdirs()
                        } else {
                            destFile.parentFile?.mkdirs()
                            destFile.outputStream().buffered().use {
                                tarIn.copyTo(it)
                            }
                        }
                        entry = tarIn.nextTarEntry
                    }
                }
            }
            tempFile.delete()
        }
    }

    private fun broadcastUpdate(url: String, status: Int, progress: Int = 0, error: String? = null) {
        val intent = Intent(ACTION_SDK_MANAGER_UPDATE).apply {
            putExtra(EXTRA_URL, url)
            putExtra(EXTRA_STATUS, status)
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_ERROR, error)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createNotification(title: String, text: String, progress: Int): Notification {
        val intent = Intent(this, SdkManagerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress.coerceAtLeast(0), progress < 0)
            .build()
    }
    
    private fun updateNotification(packageName: String, status: String, progress: Int) {
        val notification = createNotification(packageName, status, progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SDK下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示SDK组件的下载和安装进度"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "SdkManagerServiceChannel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_SDK_MANAGER_UPDATE = "com.itsaky.androidide.SDK_MANAGER_UPDATE"

        const val EXTRA_URL = "extra_url"
        const val EXTRA_DEST_SUBPATH = "extra_dest_subpath"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_ERROR = "extra_error"

        const val STATUS_STARTED = 0
        const val STATUS_PROGRESS = 1
        const val STATUS_EXTRACTING = 2
        const val STATUS_COMPLETED = 3
        const val STATUS_FAILED = 4
    }
}