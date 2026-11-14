package com.statushd.app.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.statushd.app.R
import com.statushd.app.config.AppConfig
import com.statushd.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * StatusHD — servis mampatan latar (foreground, jenis mediaProcessing).
 *
 * Mampatan berjalan di sini supaya tidak mati apabila pengguna keluar dari
 * skrin pemprosesan. Setiap sesi FFmpegKit dikemas kini ke Room; UI hanya
 * memerhati Flow kerja (tiada broadcast).
 *
 * Input MediaStore (content://) disalin dahulu ke cache kerana FFmpeg
 * hanya membaca laluan fail. Output kekal dalam cache sehingga pengguna
 * menyimpan ke galeri atau berkongsi.
 */
class CompressService : Service() {

    companion object {
        const val ACTION_START = "com.statushd.app.action.START"
        const val ACTION_CANCEL = "com.statushd.app.action.CANCEL"
        const val EXTRA_JOB_ID = "job_id"
        const val CHANNEL_ID = "statushd_compress"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, jobId: String) {
            val intent = Intent(context, CompressService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_JOB_ID, jobId)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context, jobId: String) {
            val intent = Intent(context, CompressService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startService(intent)
        }

        fun outputFile(context: Context, jobId: String, partIndex: Int = 0): File {
            val dir = File(context.cacheDir, AppConfig.OUTPUT_DIR).apply { mkdirs() }
            val suffix = if (partIndex > 0) "_p$partIndex" else ""
            return File(dir, "$jobId$suffix.mp4")
        }

        fun inputFile(context: Context, jobId: String, ext: String): File {
            val dir = File(context.cacheDir, AppConfig.OUTPUT_DIR).apply { mkdirs() }
            return File(dir, "${jobId}_input.$ext")
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningSessions = mutableMapOf<String, Long>()
    private val lastErrors = mutableMapOf<String, String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId = intent?.getStringExtra(EXTRA_JOB_ID) ?: return START_NOT_STICKY
        when (intent.action) {
            ACTION_CANCEL -> {
                runningSessions[jobId]?.let { FFmpegKit.cancel(it) }
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification(0, ""))
                scope.launch { runJob(jobId) }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun runJob(jobId: String) {
        val db = AppDatabase.get(this)
        val job = db.jobDao().getById(jobId) ?: run { stopSelf(); return }
        val media = db.mediaItemDao().getById(job.mediaId) ?: run {
            db.jobDao().markFailed(jobId, "media-missing")
            stopSelf()
            return
        }
        db.jobDao().updateStatus(jobId, "running", 0)
        try {
            // Salin input ke cache (FFmpeg perlukan laluan fail).
            val ext = if (media.type == "photo") "jpg" else "mp4"
            val inputFile = inputFile(this, jobId, ext)
            copyToFile(Uri.parse(media.uri), inputFile)

            val preset = Preset.fromName(job.preset)
            val aspect = if (job.aspect == "9:16") Aspect.NINE_16 else Aspect.ORIGINAL
            val displayW = if (job.metaRotation == 90 || job.metaRotation == 270) {
                job.metaHeight
            } else {
                job.metaWidth
            }
            val displayH = if (job.metaRotation == 90 || job.metaRotation == 270) {
                job.metaWidth
            } else {
                job.metaHeight
            }

            if (job.clipLengthMs > 0 && job.durationMs > AppConfig.SPLIT_PART_MS) {
                runSplit(jobId, inputFile, preset, aspect, displayW, displayH)
            } else if (media.type == "photo") {
                val output = outputFile(this, jobId)
                val command = FfmpegCommandBuilder.photoCommand(
                    inputFile.absolutePath, output.absolutePath, preset,
                )
                val ok = runSession(jobId, command, job.durationMs)
                finishJob(jobId, ok, output)
            } else {
                val output = outputFile(this, jobId)
                val command = FfmpegCommandBuilder.compressCommand(
                    inputFile.absolutePath, output.absolutePath,
                    preset, aspect, displayW, displayH,
                )
                val ok = runSession(jobId, command, job.durationMs)
                finishJob(jobId, ok, output)
            }
        } catch (e: Exception) {
            db.jobDao().markFailed(jobId, e.message ?: "error")
            notifyDone(jobId, false)
        }
        stopSelf()
    }

    private suspend fun runSplit(
        jobId: String,
        inputFile: File,
        preset: Preset,
        aspect: Aspect,
        displayW: Int,
        displayH: Int,
    ) {
        val db = AppDatabase.get(this)
        val currentParts = currentSplitParts(jobId)
        var allOk = true
        var index = 0
        for (part in currentParts) {
            index++
            if (part.status == "delivered") continue
            db.splitPartDao().updateStatus(part.id, "running", null)
            val output = outputFile(this, jobId, part.partIndex + 1)
            val command = FfmpegCommandBuilder.splitCommand(
                inputFile.absolutePath, output.absolutePath,
                preset, aspect, displayW, displayH,
                startMs = part.startMs,
                partMs = part.endMs - part.startMs,
            )
            val ok = runSession(jobId, command, part.endMs - part.startMs)
            if (ok && output.exists()) {
                db.splitPartDao().updateStatus(part.id, "delivered", output.absolutePath)
            } else {
                db.splitPartDao().updateStatus(part.id, "failed", null)
                allOk = false
            }
            // Kemajuan keseluruhan kerja = bahagian siap / jumlah bahagian.
            val percent = (index * 100 / currentParts.size.coerceAtLeast(1))
            db.jobDao().updateProgress(jobId, percent)
            updateNotification(percent, jobId)
        }
        if (allOk) {
            db.jobDao().updateStatus(jobId, "delivered", 100)
            notifyDone(jobId, true)
        } else {
            db.jobDao().markFailed(jobId, lastErrors[jobId] ?: "split-failed")
            notifyDone(jobId, false)
        }
    }

    private suspend fun currentSplitParts(
        jobId: String,
    ): List<com.statushd.app.data.local.SplitPartEntity> {
        // Room tidak menyediakan get suspend untuk senarai ini; baca melalui
        // Flow sekali dengan first().
        return kotlinx.coroutines.flow.first(
            AppDatabase.get(this).splitPartDao().observeByJob(jobId),
        )
    }

    /** Jalankan satu sesi FFmpegKit; kembalikan true jika berjaya. */
    private suspend fun runSession(jobId: String, command: String, totalMs: Long): Boolean {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val session = FFmpegKit.executeAsync(
                command,
                { s ->
                    val rc = s.returnCode
                    val ok = ReturnCode.isSuccess(rc)
                    runningSessions.remove(jobId)
                    if (!ok && ReturnCode.isCancel(rc)) {
                        lastErrors[jobId] = "cancelled"
                    }
                    if (cont.isActive) cont.resume(ok)
                },
                { log ->
                    val msg = log.message
                    if (msg.isNotBlank()) lastErrors[jobId] = msg.takeLast(200)
                },
                { stats ->
                    val percent = FfmpegCommandBuilder.progressPercent(stats.time, totalMs)
                    scope.launch {
                        val db = AppDatabase.get(this@CompressService)
                        db.jobDao().updateProgress(jobId, percent)
                        updateNotification(percent, jobId)
                    }
                },
            )
            runningSessions[jobId] = session.sessionId
            cont.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
        }
    }

    private suspend fun finishJob(jobId: String, ok: Boolean, output: File) {
        val db = AppDatabase.get(this)
        if (ok && output.exists()) {
            db.jobDao().markDelivered(jobId, "delivered", output.absolutePath, output.length())
            notifyDone(jobId, true)
        } else {
            val error = lastErrors[jobId] ?: "ffmpeg-failed"
            db.jobDao().markFailed(jobId, error)
            notifyDone(jobId, false)
        }
    }

    private suspend fun copyToFile(uri: Uri, target: File) = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("cannot-open-input")
    }

    private fun updateNotification(progress: Int, jobId: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(progress, jobId))
    }

    private fun notifyDone(jobId: String, ok: Boolean) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val text = if (ok) {
            getString(R.string.notif_done)
        } else {
            getString(R.string.notif_failed)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID + 1, notification)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(progress: Int, jobId: String): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.notif_channel_desc) }
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_compressing, jobId.take(8)))
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
