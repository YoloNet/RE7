# StatusHD

Mampat video untuk Status WhatsApp — HD, tanpa kabur. **100% percuma.**

Clean-room rebuild of PureStatus' *functions* (from `replica/recon.md` +
`replica/rea-analysis.md`). Every line of code here is written fresh;
never their code, assets, brand, endpoints or pricing.

## Status

**v1.0.0 (versionCode 1)** — M0 vertical slice + M1 must-haves built:

- Home (mode cards + recent), media picker (grid + search + filters),
  compression options (720p / 1080p / 4K, Original / 9:16, caption),
  processing (foreground service, progress, cancel), result
  (before/after preview, share to WhatsApp, save to gallery,
  send to my number), 30-second splitter, support (FAQ + email),
  settings (language BM/EN, default quality, privacy).
- On-device compression with FFmpegKit — no account, no ads,
  no tracking, no server in phase 1.
- Tall videos (9:20/9:21) get a **blurred background pad, not black bars**
  (the original's top bug, fixed here by design).
- Split parts are encoded independently (the original's clip-2 freeze
  is avoided by construction).

Not yet built (next steps): **M2 editor** (trim, text overlays, stickers,
music), **M3 AI Studio** (phase 2, server-side). See `replica/build-log.md`.

## Build

Open this folder in **Android Studio** (Rabbit 2 / 2026.2.2 or newer),
sync Gradle, Run. Gradle 9.8.2 + AGP 9.5.0 + Kotlin 2.4.10 + JDK 21.

- applicationId `com.statushd.app` (working name — rename before launch).
- compileSdk / targetSdk 36, minSdk 24.
- No `gradlew` in the repo (Android Studio's own wrapper is used).
- Pure-Kotlin logic tests (FFmpeg command builder): `bash tools/run-tests.sh`
  — 35 checks, no Android SDK needed.

## Known limits (honest)

- The APK is large (~70 MB+): FFmpegKit `full` ships native libs for
  all ABIs, same as the original (62.7 MB).
- The launcher icon is adaptive XML only; on API 24/25 the default
  Android icon shows. Add PNGs via Image Asset Studio if that matters.
- Inter font is not bundled (binary font files cannot be pushed here);
  the app uses the system font until the TTFs are added to `res/font/`.
- "Share all" for the splitter shares part 1 and asks the user to
  repeat in order — WhatsApp has no multi-video Status share.
- "Send to my number" opens a WhatsApp chat with your own number;
  you then attach the video from the share sheet (v1 approximation
  of the original's delivery).
- WhatsApp-target bitrate presets are estimates (CRF 24/22/20 +
  maxrate caps); measure the real output on a phone and tune if
  WhatsApp still re-compresses visibly.
- The build has **not** been verified by a real Gradle build yet
  (Gradle cannot run in this VM — loopback blocked). First build
  is on Jean's PC.

## Layout

```
statushd/
  settings.gradle.kts  build.gradle.kts  gradle.properties
  gradle/libs.versions.toml
  app/                  # Android module (Kotlin + Jetpack Compose)
    src/main/
      AndroidManifest.xml
      res/values/strings.xml  res/values-en/strings.xml
      java/com/statushd/app/
        MainActivity.kt  StatusHdApp.kt
        config/  data/local/  data/media/
        media/ (FfmpegCommandBuilder, CompressService)
        ui/theme/  ui/components/  ui/screens/  ui/navigation/
        viewmodel/
  tools/                # JVM tests for the pure-Kotlin logic
  replica/              # recon, architecture, schema, design, build-log
```

## Rules

- Never build a MOD APK / premium unlock of the original.
- No paywall, no subscriptions, no free-use limit, no ads, no watermark.
- No tracking SDKs, no INTERNET permission in phase 1.
