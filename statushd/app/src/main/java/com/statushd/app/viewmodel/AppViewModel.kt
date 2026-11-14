package com.statushd.app.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statushd.app.config.AppConfig
import com.statushd.app.data.local.AppDatabase
import com.statushd.app.data.local.CompressionJobEntity
import com.statushd.app.data.local.MediaItemEntity
import com.statushd.app.data.local.Settings
import com.statushd.app.data.local.SettingsStore
import com.statushd.app.data.local.SplitPartEntity
import com.statushd.app.data.media.MediaItem
import com.statushd.app.data.media.MediaProbe
import com.statushd.app.data.media.MediaStoreRepo
import com.statushd.app.media.CompressService
import com.statushd.app.media.FfmpegCommandBuilder
import com.statushd.app.ui.navigation.PickerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * StatusHD — ViewModel tunggal (seperti KebunKu native).
 * Mengorkestra Room + DataStore + MediaStore + CompressService.
 * UI hanya memerhati Flow; tiada logik FFmpeg di skrin.
 */
class AppViewModel(private val context: Context) : ViewModel() {

    private val db = AppDatabase.get(context)
    private val settingsStore = SettingsStore(context)
    private val mediaRepo = MediaStoreRepo(context)

    val settings: StateFlow<Settings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val recentJobs: StateFlow<List<CompressionJobEntity>> =
        db.jobDao().observeRecentDelivered(6)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private val _mediaLoading = MutableStateFlow(true)
    val mediaLoading: StateFlow<Boolean> = _mediaLoading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** Cache URI media asal (mediaId -> URI kandungan) untuk togol Sebelum. */
    private val mediaUriCache = mutableMapOf<String, String>()
    fun mediaUriFor(mediaId: String): String? = mediaUriCache[mediaId]

    /** Thumbnail grid galeri (delegasi ke MediaStoreRepo). */
    suspend fun loadThumbnail(item: MediaItem): Bitmap? = mediaRepo.thumbnail(item)

    fun showToast(message: String) {
        _toast.value = message
    }

    fun clearToast() {
        _toast.value = null
    }

    init {
        viewModelScope.launch {
            settingsStore.ensureDeviceKey()
            loadMedia()
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _mediaLoading.value = true
            _mediaList.value = mediaRepo.queryAll()
            _mediaLoading.value = false
        }
    }

    fun observeJob(jobId: String): Flow<CompressionJobEntity?> =
        db.jobDao().observeById(jobId)

    fun observeParts(jobId: String) = db.splitPartDao().observeByJob(jobId)

    /** Simpan item media terpilih ke Room; kembalikan mediaId. */
    suspend fun registerMedia(item: MediaItem): String {
        mediaUriCache[item.id] = item.uri.toString()
        val entity = MediaItemEntity(
            id = item.id,
            uri = item.uri.toString(),
            type = item.type,
            durationMs = item.durationMs,
            width = item.width,
            height = item.height,
            sizeBytes = item.sizeBytes,
            dateAdded = item.dateAdded,
        )
        db.mediaItemDao().insert(entity)
        return item.id
    }

    /**
     * Cipta kerja mampatan dan mulakan servis.
     * Kembalikan jobId. Untuk SPLIT, bahagian dicipta di sini.
     */
    fun startJob(
        mediaId: String,
        mode: PickerMode,
        preset: String,
        aspect: String,
        caption: String?,
    ): String {
        val jobId = UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            val media = db.mediaItemDao().getById(mediaId) ?: return@launch
            val meta = MediaProbe.probe(context, Uri.parse(media.uri))
            val isSplit = mode == PickerMode.SPLIT
            val clipLength = if (isSplit) AppConfig.SPLIT_PART_MS else 0L
            val job = CompressionJobEntity(
                id = jobId,
                mediaId = mediaId,
                preset = preset,
                aspect = aspect,
                metaWidth = meta.width,
                metaHeight = meta.height,
                metaRotation = meta.rotation,
                clipLengthMs = clipLength,
                durationMs = if (media.type == "photo") {
                    AppConfig.PHOTO_VIDEO_SECONDS * 1000
                } else {
                    meta.durationMs
                },
                sizeBefore = media.sizeBytes,
                caption = caption?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
            )
            db.jobDao().insert(job)
            if (isSplit) {
                val total = meta.durationMs
                val parts = mutableListOf<SplitPartEntity>()
                var start = 0L
                var index = 0
                while (start < total) {
                    val end = minOf(start + AppConfig.SPLIT_PART_MS, total)
                    parts += SplitPartEntity(
                        id = "$jobId-p$index",
                        jobId = jobId,
                        partIndex = index,
                        startMs = start,
                        endMs = end,
                    )
                    start = end
                    index++
                }
                db.splitPartDao().insertAll(parts)
            }
            CompressService.start(context, jobId)
        }
        return jobId
    }

    fun cancelJob(jobId: String) {
        CompressService.cancel(context, jobId)
    }

    /** Anggaran saiz output untuk helaian pilihan. */
    fun estimateBytes(durationMs: Long, presetName: String): Long {
        val preset = com.statushd.app.media.Preset.fromName(presetName)
        return FfmpegCommandBuilder.estimateOutputBytes(durationMs, preset)
    }

    // ---- Kongsi & simpan ----

    /** Pakej WhatsApp yang dipasang (utama dahulu). */
    fun installedWhatsapp(): String? {
        val pm = context.packageManager
        return AppConfig.WHATSAPP_PACKAGES.firstOrNull { pkg ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /** Intent kongsi video output ke WhatsApp (Status atau sembang). */
    fun buildShareIntent(job: CompressionJobEntity, partPath: String? = null): Intent? {
        val pkg = installedWhatsapp() ?: return null
        val path = partPath ?: job.outputUri ?: return null
        val file = File(path)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            setPackage(pkg)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            job.caption?.let { putExtra(Intent.EXTRA_TEXT, it) }
        }
    }

    /** Intent buka sembang WhatsApp dengan nombor sendiri (untuk hantaran). */
    fun buildMyNumberIntent(number: String): Intent? {
        val clean = number.filter { it.isDigit() }
        if (clean.length < 9) return null
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$clean"),
        )
    }

    /** Simpan fail output ke galeri (Movies/StatusHD). Kembalikan URI atau null. */
    suspend fun saveToGallery(job: CompressionJobEntity, partPath: String? = null): Uri? =
        withContext(Dispatchers.IO) {
            val path = partPath ?: job.outputUri ?: return@withContext null
            val file = File(path)
            if (!file.exists()) return@withContext null
            val displayName = "StatusHD_${job.id.take(8)}" +
                (if (partPath != null) "_${File(partPath).nameWithoutExtension}" else "") +
                ".mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "Movies/${AppConfig.GALLERY_FOLDER}",
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                } else {
                    @Suppress("DEPRECATION")
                    put(
                        MediaStore.Video.Media.DATA,
                        "${android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_MOVIES,
                        )}/${AppConfig.GALLERY_FOLDER}/$displayName",
                    )
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values,
            ) ?: return@withContext null
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } catch (_: Exception) {
                try {
                    resolver.delete(uri, null, null)
                } catch (_: Exception) {
                }
                null
            }
        }

    // ---- Tetapan ----

    fun setLanguage(language: String) {
        viewModelScope.launch { settingsStore.setLanguage(language) }
    }

    fun setQualityDefault(quality: String) {
        viewModelScope.launch { settingsStore.setQualityDefault(quality) }
    }

    fun setPrivacySeen() {
        viewModelScope.launch { settingsStore.setPrivacySeen(true) }
    }

    /** Tunjuk semula notis privasi di Home (untuk Tetapan). */
    fun resetPrivacyNotice() {
        viewModelScope.launch { settingsStore.setPrivacySeen(false) }
    }

    fun setWhatsappNumber(number: String) {
        viewModelScope.launch { settingsStore.setWhatsappNumber(number) }
    }
}
