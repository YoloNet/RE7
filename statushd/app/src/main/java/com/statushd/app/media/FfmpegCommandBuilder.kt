package com.statushd.app.media

/**
 * StatusHD — pembina perintah FFmpeg (Kotlin tulen, tiada import Android).
 *
 * Fail ini diuji atas JVM (lihat tools/test di repo) kerana logiknya tidak
 * bergantung pada Android. Perintah yang dibina diumpan ke FFmpegKit
 * (com.arthenica.ffmpegkit) melalui CompressService.
 *
 * Keputusan reka bentuk (architecture.md):
 * - Output MP4 (H.264 + AAC), moov di depan (+faststart), yuv420p.
 * - Video tinggi (nisbah lebih tinggi daripada 9:16, cth. 9:20/9:21)
 *   dipadankan dengan latar kabur, BUKAN bar hitam (pepijat original).
 * - Nisbah 9:16 juga guna latar kabur supaya tiada kandungan terpotong.
 * - Audio adalah pilihan (`-map 0:a:0?`): video tanpa audio tidak gagal.
 * - ffmpeg memutar video automatik mengikut metadata putaran, jadi
 *   [displayW]/[displayH] yang diberi mestilah saiz PAPARAN (selepas
 *   putaran), bukan saiz storan.
 */
enum class Preset(val width: Int, val height: Int, val crf: Int, val maxRateMbps: Int) {
    P720(720, 1280, 24, 5),
    P1080(1080, 1920, 22, 10),
    P4K(2160, 3840, 20, 25);

    companion object {
        fun fromName(name: String): Preset = when (name) {
            "1080p" -> P1080
            "4k" -> P4K
            else -> P720
        }
    }
}

enum class Aspect { ORIGINAL, NINE_16 }

object FfmpegCommandBuilder {

    /** Benar jika sumber lebih tinggi daripada 9:16 (cth. 9:20, 9:21). */
    fun isTall(displayW: Int, displayH: Int): Boolean {
        if (displayW <= 0 || displayH <= 0) return false
        return displayH * 9 > displayW * 16
    }

    /**
     * Penapis video untuk satu output. Mengembalikan rantaian -vf.
     * - 9:16, atau sumber tinggi + ORIGINAL: latar kabur (blur-pad).
     * - ORIGINAL biasa: muat dalam kotak preset, kekalkan nisbah.
     */
    fun videoFilter(preset: Preset, aspect: Aspect, displayW: Int, displayH: Int): String {
        val w = preset.width
        val h = preset.height
        val blurPad = aspect == Aspect.NINE_16 || isTall(displayW, displayH)
        return if (blurPad) {
            "[0:v]split[a][b];" +
                "[a]scale=$w:$h:force_original_aspect_ratio=decrease," +
                "pad=$w:$h:(ow-iw)/2:(oh-ih)/2[fg];" +
                "[b]scale=$w:$h:force_original_aspect_ratio=increase," +
                "crop=$w:$h,gblur=sigma=40[bg];" +
                "[bg][fg]overlay=(W-w)/2:(H-h)/2,format=yuv420p"
        } else {
            "scale=$w:$h:force_original_aspect_ratio=decrease,format=yuv420p"
        }
    }

    /** Perintah mampat penuh (video). */
    fun compressCommand(
        inputPath: String,
        outputPath: String,
        preset: Preset,
        aspect: Aspect,
        displayW: Int,
        displayH: Int,
    ): String {
        val filter = videoFilter(preset, aspect, displayW, displayH)
        return "-y -i \"$inputPath\" -vf \"$filter\" " +
            "-map 0:v:0 -map 0:a:0? " +
            "-c:v libx264 -preset veryfast -crf ${preset.crf} " +
            "-maxrate ${preset.maxRateMbps}M -bufsize ${preset.maxRateMbps * 2}M " +
            "-pix_fmt yuv420p -movflags +faststart " +
            "-c:a aac -b:a 128k \"$outputPath\""
    }

    /** Perintah satu bahagian splitter (mula pada [startMs], selama [partMs]). */
    fun splitCommand(
        inputPath: String,
        outputPath: String,
        preset: Preset,
        aspect: Aspect,
        displayW: Int,
        displayH: Int,
        startMs: Long,
        partMs: Long = 30_000L,
    ): String {
        val filter = videoFilter(preset, aspect, displayW, displayH)
        val startSec = startMs / 1000.0
        val partSec = partMs / 1000.0
        return "-y -ss $startSec -t $partSec -i \"$inputPath\" -vf \"$filter\" " +
            "-map 0:v:0 -map 0:a:0? " +
            "-c:v libx264 -preset veryfast -crf ${preset.crf} " +
            "-maxrate ${preset.maxRateMbps}M -bufsize ${preset.maxRateMbps * 2}M " +
            "-pix_fmt yuv420p -movflags +faststart " +
            "-c:a aac -b:a 128k \"$outputPath\""
    }

    /** Perintah foto → video HD senyap (lalai 5 saat). */
    fun photoCommand(
        inputPath: String,
        outputPath: String,
        preset: Preset,
        durationSec: Long = 5L,
    ): String {
        val w = preset.width
        val h = preset.height
        val filter = "[0:v]split[a][b];" +
            "[a]scale=$w:$h:force_original_aspect_ratio=decrease," +
            "pad=$w:$h:(ow-iw)/2:(oh-ih)/2[fg];" +
            "[b]scale=$w:$h:force_original_aspect_ratio=increase," +
            "crop=$w:$h,gblur=sigma=40[bg];" +
            "[bg][fg]overlay=(W-w)/2:(H-h)/2,format=yuv420p"
        return "-y -loop 1 -framerate 30 -t $durationSec -i \"$inputPath\" " +
            "-vf \"$filter\" " +
            "-c:v libx264 -preset veryfast -crf 23 " +
            "-pix_fmt yuv420p -movflags +faststart " +
            "-f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 " +
            "-map 0:v:0 -map 1:a:0 -c:a aac -b:a 96k -shortest \"$outputPath\""
    }

    /**
     * Anggaran saiz output (bait) — kasar: 60% maxrate video + 128k audio.
     * Dilabel "Anggaran" di UI; saiz sebenar bergantung pada kandungan.
     */
    fun estimateOutputBytes(durationMs: Long, preset: Preset): Long {
        if (durationMs <= 0) return 0L
        val seconds = durationMs / 1000.0
        val bitsPerSecond = preset.maxRateMbps * 1_000_000.0 * 0.6 + 128_000.0
        return (seconds * bitsPerSecond / 8.0).toLong()
    }

    /** 26_000_000 -> "24.8 MB". */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    /** 24_000 -> "0:24", 95_000 -> "1:35". */
    fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    /** Kemajuan 0..100 daripada masa terkod FFmpeg. */
    fun progressPercent(encodedMs: Long, totalMs: Long): Int {
        if (totalMs <= 0 || encodedMs <= 0) return 0
        return ((encodedMs * 100) / totalMs).toInt().coerceIn(0, 100)
    }
}
