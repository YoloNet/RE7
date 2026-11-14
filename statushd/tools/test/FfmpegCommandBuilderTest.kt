package com.statushd.app.media

/**
 * Ujian JVM untuk FfmpegCommandBuilder (Kotlin tulen, tiada Android).
 * Jalankan: lihat tools/run-tests.sh. Keluar 0 jika semua lulus.
 */
fun main() {
    var passed = 0
    var failed = 0
    fun check(name: String, condition: Boolean) {
        if (condition) {
            passed++
            println("PASS: $name")
        } else {
            failed++
            println("FAIL: $name")
        }
    }
    fun contains(cmd: String, fragment: String, name: String) =
        check(name, cmd.contains(fragment))
    fun notContains(cmd: String, fragment: String, name: String) =
        check(name, !cmd.contains(fragment))

    // 1-3. isTall
    check("isTall 1080x2400 (9:21) = true", FfmpegCommandBuilder.isTall(1080, 2400))
    check("isTall 1920x1080 (landscape) = false", !FfmpegCommandBuilder.isTall(1920, 1080))
    check("isTall 720x1280 (exact 9:16) = false", !FfmpegCommandBuilder.isTall(720, 1280))
    check("isTall 0x0 = false", !FfmpegCommandBuilder.isTall(0, 0))

    // 4-6. videoFilter
    val filter916 = FfmpegCommandBuilder.videoFilter(Preset.P720, Aspect.NINE_16, 1920, 1080)
    contains(filter916, "gblur=sigma=40", "9:16 landscape uses blur-pad")
    contains(filter916, "720:1280", "9:16 target is 720x1280")
    contains(filter916, "overlay", "9:16 has overlay")
    val filterOrigLand = FfmpegCommandBuilder.videoFilter(Preset.P720, Aspect.ORIGINAL, 1920, 1080)
    contains(filterOrigLand, "scale=720:1280:force_original_aspect_ratio=decrease", "ORIGINAL landscape fits box")
    notContains(filterOrigLand, "gblur", "ORIGINAL landscape has no blur")
    val filterOrigTall = FfmpegCommandBuilder.videoFilter(Preset.P720, Aspect.ORIGINAL, 1080, 2400)
    contains(filterOrigTall, "gblur=sigma=40", "ORIGINAL tall video gets blur-pad (bug fix)")

    // 7-11. compressCommand
    val cmd = FfmpegCommandBuilder.compressCommand(
        "/in/video.mp4", "/out/video.mp4", Preset.P720, Aspect.ORIGINAL, 1920, 1080,
    )
    contains(cmd, "-c:v libx264", "compress uses libx264")
    contains(cmd, "-crf 24", "720p CRF 24")
    contains(cmd, "-maxrate 5M", "720p maxrate 5M")
    contains(cmd, "-movflags +faststart", "faststart for streaming")
    contains(cmd, "-map 0:a:0?", "audio optional (no-audio videos work)")
    contains(cmd, "\"/out/video.mp4\"", "output path quoted")
    val cmd1080 = FfmpegCommandBuilder.compressCommand(
        "/in/v.mp4", "/out/v.mp4", Preset.P1080, Aspect.NINE_16, 1080, 1920,
    )
    contains(cmd1080, "-crf 22", "1080p CRF 22")
    contains(cmd1080, "-maxrate 10M", "1080p maxrate 10M")

    // 12-13. splitCommand
    val split = FfmpegCommandBuilder.splitCommand(
        "/in/v.mp4", "/out/p2.mp4", Preset.P720, Aspect.ORIGINAL, 1920, 1080,
        startMs = 30_000L, partMs = 30_000L,
    )
    contains(split, "-ss 30.0", "split part 2 starts at 30s")
    contains(split, "-t 30.0", "split part length 30s")

    // 14-16. photoCommand
    val photo = FfmpegCommandBuilder.photoCommand("/in/foto.jpg", "/out/foto.mp4", Preset.P720)
    contains(photo, "-loop 1", "photo loops input")
    contains(photo, "-t 5", "photo video is 5s")
    contains(photo, "anullsrc", "photo video has silent audio")
    contains(photo, "-shortest", "photo video ends with video")

    // 17-18. estimateOutputBytes
    val est = FfmpegCommandBuilder.estimateOutputBytes(30_000L, Preset.P720)
    // 30s × (5 Mbps × 0.6 + 128 kbps) / 8 ≈ 11.7 MB
    check("estimate 30s 720p is sane (1-15 MB)", est in 1_000_000..15_000_000)
    check("estimate 0ms = 0", FfmpegCommandBuilder.estimateOutputBytes(0, Preset.P720) == 0L)

    // 19-22. formatBytes / formatDuration
    check("formatBytes 26_000_000 = 24.8 MB", FfmpegCommandBuilder.formatBytes(26_000_000) == "24.8 MB")
    check("formatBytes 500 = 500 B", FfmpegCommandBuilder.formatBytes(500) == "500 B")
    check("formatDuration 95000 = 1:35", FfmpegCommandBuilder.formatDuration(95_000) == "1:35")
    check("formatDuration 24000 = 0:24", FfmpegCommandBuilder.formatDuration(24_000) == "0:24")

    // 23-25. progressPercent
    check("progress 15000/30000 = 50", FfmpegCommandBuilder.progressPercent(15_000, 30_000) == 50)
    check("progress 0/0 = 0", FfmpegCommandBuilder.progressPercent(0, 0) == 0)
    check("progress over 100 clamps to 100", FfmpegCommandBuilder.progressPercent(40_000, 30_000) == 100)

    // 26-27. Preset.fromName
    check("fromName 1080p", Preset.fromName("1080p") == Preset.P1080)
    check("fromName unknown defaults to 720p", Preset.fromName("bogus") == Preset.P720)

    println("----")
    println("Passed: $passed, Failed: $failed")
    if (failed > 0) kotlin.system.exitProcess(1)
}
