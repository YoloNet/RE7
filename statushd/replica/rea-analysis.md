# REA Analysis — PureStatus APK (DamTech Designs)

Tarikh: 2026-11-14
Sumber: https://github.com/YoloNet/RE7/releases/download/V1/PureSt.apks
Fail tempatan: `~/workspace/rea-tools/test/purestatus/`
Kaedah: analisis statik sahaja (REA 6.9.0: JADX bridge 0.7.1 + inspect_apk_signature). App TIDAK dijalankan. Tiada kod disalin — laporan ini merumus struktur dan teknologi sahaja (clean-room).

## 1. Identiti (disahkan dari binari)

| Perkara | Nilai |
|---|---|
| App | PureStatus ("ByeBye Blur Status"), DamTech Designs |
| Package | `com.damtechdesigns.purepixel` |
| Version | 2026.25 (versionCode 155) |
| minSdk / targetSdk / compileSdk | 29 / 37 / 37 |
| Tandatangan | v2 + v3, 1 signer (cert SHA-256 `c8a1c785…678a3`, sama untuk v2 dan v3); tiada v1, tiada rotation; Play source stamp hadir |
| base.apk SHA-256 | `5cc4f1aa0af9f0ac788994fce4e6b162228316056123fa821f7c1c40dcf483f8` |
| Fail .apks SHA-256 | `cd0aa8ad0d704b0df4c728f7cd72974e7c96155d649e836b10cd4c9c5c593a9b` |

Nota: REA membaca rekod tandatangan yang diisytihar sahaja — tandatangan TIDAK disahkan secara kriptografi, jadi identiti penerbit tidak dapat dipastikan dari sini. APKCube.txt dalam bundle mengesahkan `pkg=com.damtechdesigns.purepixel vc=155`.

## 2. Struktur bundle (APKS, 45.1 MB, 31 fail)

- `base.apk` 41.2 MB — kod + aset utama
- `split_config.arm64_v8a.apk` 37.0 MB — semua native library
- Split bahasa: de, en, es, fi, fr, hi, hu, in, it, ja, ko, **ms (Melayu)**, nl, pl, pt, ru, sv, th, tr, uk, vi, zh — 60 bahasa disahkan wujud dalam bentuk split
- Split densiti: mdpi, tvdpi, hdpi, xxhdpi, xxxhdpi; `split_config.ar.apk`
- base.apk: 3 fail DEX (classes.dex 9.8 MB, classes2.dex 10.5 MB, classes3.dex 3.3 MB), **25,778 kelas**, 1,362 resources

## 3. Kod sendiri sangat kecil — app ini 99% SDK

Hanya **51 kelas** di bawah `com.damtechdesigns.purepixel` (0.2% daripada semua kelas). Selebihnya SDK pihak ketiga. Kod sendiri di-obfuscate penuh dengan R8 (nama method jadi `a`, `b`, `c`…), jadi logik dalaman tepat tidak dapat dibaca tanpa decompile penuh.

Struktur kod sendiri:
- `general.*` — model data + servis: `PurePixelApp`, `BackgroundCompressService`, `CompressJob`, `UserData`, `StickerData/Pack`, `TextOverlayData`, `TrimData`, `MusicData`, `AudioData/Category`, `MyImages`, `FeaturedStickerPack`, `AlertData`, `NumberList`
- `views.*` — 21 aktiviti (satu aktiviti setiap skrin utama)
- `views.ai.*` — 5 aktiviti AI Studio: `AIHomeView`, `AICreateView`, `AICreationsView`, `AIBuyTokensView`, `AIResultView`
- `ai.*` — `AIJob`, `AIStickerContentProvider` (ContentProvider untuk kongsi sticker pack ke WhatsApp)
- Kelas khas wilayah: `RUData`, `RUSubscriptionData`, `RUScreenshotData`, `RUUserData`, `FetchRUDataResponse` (Rusia) + flag `isTurkey` dalam CompressJob (Turki) — laluan langganan/pembayaran berbeza ikut negara

## 4. Skrin utama (dari AndroidManifest — 98 aktiviti, 18 servis, 13 receiver, 11 provider)

Skrin app sendiri: `StatusView`, `MainView`, `SplashActivity/SplashView`, `QueueView`, `CompressView`, `VideoTrimView`, `VideoEditView`, `StoryCreateView`, `StickersView`, `MusicSelectView`, `SettingsView`, `SubscribeView`, `SubscribeRussiaView`, `ActivateWhatsAppView`, `LanguageSelectView`, `IntroView`, `FAQView`, `SupportView`, `ChatSupportView`, `AboutView`, `WebViewActivity`, `UpdateRequiredView`.

**27 aktiviti festival khusus** (`Festival_diwali`, `Festival_christmas`, `Festival_eid`, `Festival_ramadan`, `Festival_cny`, `Festival_holi`, `Festival_halloween`, `Festival_valentine`, `Festival_newyear`, `Festival_easter`, `Festival_maslenitsa`, `Festival_cosmonautics`, `Festival_defender`, `Festival_victory`, `Festival_womensday`, `Festival_mothersday`, `Festival_fathersday`, `Festival_orthodox_christmas`, `Festival_belarus_day`, `Festival_russia_day`, `Festival_knowledge_day`, `Festival_kupalle`, `Festival_eidadha`, `Festival_independence_pk/my/in/id/ng`) — termasuk `independence_my` (Hari Merdeka Malaysia). Katalog dalam app pula menyenaraikan **176 festival** (lihat §7).

## 5. Enjin media — kompresi guna FFmpeg

Split arm64 mengandungi stack FFmpeg penuh:
- `libavcodec.so` 17.9 MB, `libavformat.so` 2.2 MB, `libavfilter.so` 3.5 MB, `libavutil.so`, `libswscale.so`, `libswresample.so`, `libavdevice.so`
- `libffmpegkit.so` + `libffmpegkit_abidetect.so` — **FFmpegKit (com.arthenica.ffmpegkit)** ialah wrapper yang dipakai
- Kesimpulan: kompresi video dibuat on-device dengan FFmpeg melalui FFmpegKit, bukan MediaCodec semata-mata. Ini perpustakaan open-source (LGPL) — boleh diguna dalam clone.

Servis: `BackgroundCompressService` — foreground service (progress notification) yang memproses `CompressJob` secara beratur (`QueueView`).

### Model data CompressJob (Kotlin data class, 25+ medan)

Status: `pending / running / delivered / failed`. Medan penting:
- `videoPath`, `metaWidth/metaHeight/metaRotation`, `metaHDR`, `qualityFHD`, `aspect916`
- `clipLengthMs/clipLengthS`, `durationMs`, `createdAt`, `progress`, `error`, `premium`
- `trim` (TrimData), `music` (MusicData), `stickers` (List\<StickerData\>), `texts` (List\<TextOverlayData\>), `caption`
- `senderNumber`, `whatsapp` — job tahu nombor WhatsApp sasaran untuk hantar status
- `seq` — flag bahagian berurutan (split 30 saat)
- `isTurkey` — flag khas Turki

### UserData — "akaun" tanpa pendaftaran

Hanya 2 medan: `deviceKey` + `whatsapp`. Disahkan: tiada email/nama — identiti pengguna ialah kunci peranti tanpa nama + nombor WhatsApp. Selari dengan recon (local-first, no-account).

## 6. Monetisasi, iklan, analitik (dari manifest + kelas)

- **Billing:** RevenueCat (`com.revenuecat.purchases.*`, termasuk Amazon proxy) + Play Billing (`ProxyBillingActivity/V2`). Paywall ada 2 varian: `SubscribeView` dan `SubscribeRussiaView`.
- **Anti-piracy / lesen:** PairIP — `com.pairip.licensecheck.LicenseActivity` + `libpairipcore.so`; permission `com.amazon.privacypass.ATTEST`.
- **Iklan (mediation):** AdMob, **AppLovin MAX** (aktiviti debugger MAX lengkap), Yandex Ads, Meta Audience Network, AdColony, Vungle.
- **Analitik / SDK lain:** Firebase (Auth, Analytics/Measurement, Sessions, Recaptcha), Google ML Kit, AppMetrica (Yandex), Facebook SDK.
- **WhatsApp:** manifest `queries` menyasarkan `com.whatsapp` dan `com.whatsapp.w4b` (WhatsApp Business) secara eksplisit; `ActivateWhatsAppView` mengurus pengaktifan/pilihan WhatsApp.

## 7. AI Studio — katalog data-driven (assets/ai_catalog.json)

Fail `ai_catalog.json` dibundel dalam APK (dijana dari kod iOS: `AIStudioModels.swift`, `AIFestivals.swift`, remote-config `ai_model_variants.json`). Maksudnya katalog AI Studio dikongsi antara Android dan iOS, dan boleh dikemas kini tanpa ubah kod.

- **Token:** bonus alu-aluan 200, percuma 100/bulan, premium 200/bulan, lantai 5 token, video 5–15 saat, pek 9 sticker +20 token
- **22 ciri dalam 4 seksyen** (kos token): AI Image 10, Photo Restore 20, HD Enhance 15, Background Eraser 8, Festival Photoshoot 15, Face Swap 15, AI Styles 20, Magic Eraser 20, Fit to Status 20, Passport Photo 20, AI Stickers 10, Photo to Sticker 15, Sticker Pack 60, Animated Sticker 75, Photo to Video 25 (premium), Talking Photo 40 (premium), Video HD 40 (premium), Smart Montage 15, Video Reframe 0 (percuma), Auto Captions 5, Photo Slideshow 10, Story Templates 15
- **176 festival**, senarai negara, dial codes, emoji pek sticker — semua dalam fail katalog ini
- Model on-device dibundel (`assets/models_bundled/`): blazeface, contours, fssd (ML Kit face detection/landmark) — untuk Festival Photoshoot / Face Swap
- 27 fail aset nama rawak (~160 KB setiap satu) — kandungan tidak dikenal pasti (kemungkinan pek/festival berbungkus)

## 8. Permissions (28)

Penting: `READ_MEDIA_VIDEO/IMAGES/AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` (maxSdk 32), `WRITE_EXTERNAL_STORAGE` (maxSdk 29), `INTERNET`, `BILLING`, `AD_ID`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` + `FOREGROUND_SERVICE_MEDIA_PROCESSING`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `READ_BASIC_PHONE_STATE`, `VIBRATE`, `CHECK_LICENSE`, `BIND_GET_INSTALL_REFERRER_SERVICE`, `ACCESS_ADSERVICES_*` (Attribution, Ad ID, Custom Audience, Topics), `READ_GSERVICES`, `c2dm RECEIVE`, `com.amazon.privacypass.ATTEST`.

## 9. Implikasi untuk clone (replica-architect)

Yang boleh ditiru secara clean-room (idea/arkitektur, bukan kod):
- Satu `CompressJob` + foreground service + queue — model data di atas boleh jadi rujukan struktur
- FFmpegKit + FFmpeg untuk kompresi (open-source, LGPL)
- Katalog ciri berasaskan JSON (seperti `ai_catalog.json`) untuk AI Studio / festival — senang dikemas kini
- RevenueCat untuk langganan (ganti terus Play Billing pun boleh)
- AppLovin MAX atau AdMob untuk mediation iklan

Yang TIDAK boleh disalin:
- Kod, aset, nama, teks, ikon PureStatus (copyright DamTech)
- 27 fail aset nama rawak + model/festival data mereka
- PairIP (proprietary, anti-piracy berbayar) — guna Play App Licensing / Play Integrity API sebagai ganti
- Token/pricing AI mereka ialah data perniagaan mereka — tetapkan sendiri

## 10. Privasi — apa yang dikumpul dan dihantar (disiasat 2026-11-14, dari strings DEX + manifest + dasar privasi)

**Ya, app ini mengumpul dan menghantar maklumat pengguna ke server.** Bukan malware, tetapi skopnya luas:

- **Analytics/ads (automatik, melalui SDK):** Firebase Analytics + Crashlytics (`app-measurement.com`), Yandex AppMetrica (`appmetrica.io`), Facebook SDK, AdMob/AppLovin/Vungle/AdColony/Yandex Ads — menerima Advertising ID (`AD_ID` permission + `ACCESS_ADSERVICES_*`: Attribution, Ad ID, Custom Audience, Topics), model peranti, versi OS/app, sesi dan event. Ini berjalan latar belakang tanpa paparan kepada pengguna.
- **Server sendiri:** `support.purestatus.app/api.php` (sokongan chat dalam app) dan `api-production.8-lives-cat.io/` (backend API — operator tidak dapat dikenal pasti dari strings; berkemungkinan backend AI Studio). `UserData` mengandungi `deviceKey` + `whatsapp` — nombor WhatsApp pengguna dikumpul (pengesahan "Verify your WhatsApp number to unlock AI tools", penghantaran video termampat ke nombor sendiri melalui WhatsApp API) dan dihantar ke server mereka.
- **Media:** kompresi on-device (FFmpeg) — video/foto tidak keluar dari telefon untuk kompresi. Ciri AI Studio MESTI memuat naik media yang dipilih ke server (penjanaan AI tidak boleh dibuat sepenuhnya on-device).
- **Consent:** ADA skrin consent privasi semasa first launch + Google UMP / Funding Choices (`fundingchoicesmessages.google.com`, string IAB TCF) dan dasar privasi yang mendedahkan AdMob + Firebase Analytics. Jadi pengumpulan didedahkan secara bertulis — tetapi pengguna biasa tidak nampak apa yang dihantar, dan Play Data safety menyatakan "data can't be deleted".
- **Yang TIDAK diambil:** tiada permission lokasi, kenalan, nombor telefon (`READ_PHONE_NUMBERS`), kamera atau mikrofon; tiada cleartext (HTTPS sahaja secara default); `allowBackup=true` tetapi SharedPreferences dikecualikan dari backup awan.
- **Had:** analisis statik tidak dapat membuktikan payload sebenar — itu memerlukan pemerhatian trafik runtime (peranti/emulator).

Evidence: strings DEX (`/tmp/purestatus-strings.txt`, 101,418 strings), manifest, recon.md § sources (privacy policy + Play Data safety).

## 11. Had analisis ini

- Statik sahaja; app tidak dijalankan, tiada runtime observation
- Tandatangan tidak disahkan kriptografi (rekod diisytihar sahaja)
- Kod sendiri R8-obfuscated — method servis kompresi hanya dikenali dari tandatangan (nama `a`–`n`), bukan logik dalaman
- FFmpegKit: kelas Java sesi tidak ditemui melalui carian nama kelas (hanya `FFmpegKitConfig` + `AbiDetect`); kehadiran FFmpeg dibuktikan oleh .so, bukan oleh kelas Java
- URL server / endpoint API tidak disiasat dalam pusingan ini (boleh dibuat kemudian jika perlu: carian string dalam DEX)
- Decompile penuh (jadx) tidak dijalankan — hanya metadata kelas melalui REA

Evidence: `inspect_apk_signature` ev_61c00a6b…, `inspect_android_package` ev_182f5607…, carian kelas ev_17f0c5a6… / ev_8f839160…, `inspect_android_class` BackgroundCompressService / CompressJob / UserData. Manifest penuh: `~/workspace/rea-tools/test/purestatus/AndroidManifest.xml`.
