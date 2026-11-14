# Architecture: StatusHD (a rebuild of PureStatus' core features)

Product: **StatusHD** — working title (folder `statushd`). Jean's own product to sell,
under his own name and brand. Clean-room rebuild: same functions as PureStatus,
own code, own words, own assets. Never the original's code, assets, copy, brand,
endpoints, or ad/billing accounts.

Sources: `replica/recon.md` (2026-11-13, public sources) +
`replica/rea-analysis.md` (2026-11-14, static analysis of the real APK
v2026.25/code 155 — facts marked **[REA]** are verified from the binary;
facts marked **[recon]** come from listings/reviews/policy and are not yet
verified in-app).

Date: 2026-11-14. Status: architecture approved to build from.
Update 2026-11-14: Jean decided 100% free — no paywall, no subscriptions,
no free-use limit, no ads. Paywall/billing/ads removed from this document
(stack, schema, API, build order, summary) and from `schema.sql` /
`features.csv`.
Repo: `YoloNet/RE7`, folder `statushd/` (this file lives at
`statushd/replica/architecture.md`).

## Stack

Native Android — Jean builds in Android Studio on his PC; Muse prepares code,
Jean pulls and builds (same workflow as kuv2 / pk2). The skill's web default
does not apply: this is a local-first mobile app, no web app, no server in
phase 1.

| layer | choice | why |
| --- | --- | --- |
| mobile | Native Android, Kotlin + Jetpack Compose + Material 3 | Jean's proven stack (kuv2, pk2); the original is native Android too |
| build | Gradle 9.8.2, AGP 9.5.0, Kotlin 2.4.10, targetSdk 36, compileSdk 36, minSdk 24 | Jean's verified toolchain; Play requires target API 36 (original targets 37, but 36 matches Jean's other apps) |
| media engine | FFmpegKit (`com.arthenica.ffmpegkit`) + FFmpeg native libs | **[REA]** the original compresses on-device with FFmpegKit (libavcodec 17.9 MB, libavformat/filter/util, swscale, swresample). Open-source (LGPL). Precise WhatsApp-target bitrate control is the core value — MediaCodec alone does not give the same control |
| local database | Room (SQLite) + KSP 2.3.13 | Local-first, no account. KSP 2.3.13 is the version Jean verified in kuv2 (2.4.0 does not exist) |
| settings | DataStore (Preferences) | Language, quality default, device key, free-use counter date |
| background work | Foreground Service (`FOREGROUND_SERVICE_MEDIA_PROCESSING`) + coroutines | **[REA]** the original runs compression in `BackgroundCompressService` (foreground service, progress notification). Matches `FOREGROUND_SERVICE_MEDIA_PROCESSING` permission |
| monetization | None — 100% free | Jean's decision (2026-11-14): no paywall, no subscriptions, no free-use limit, no ads. Every feature free. Consequence: no revenue from the app; phase-2 AI generation costs are Jean's cost, so any usage cap there is cost control, never a charge |
| privacy | First-launch notice (not a gate) | States the facts: no account, on-device processing, no ads, no tracking. Full text in Settings. No UMP consent form needed without ads |
| anti-piracy | None needed | No paid features to protect. The original uses PairIP (proprietary, paid) — do NOT copy |
| analytics | None in phase 1 (optional: Firebase Analytics later, own project) | The original ships Firebase + AppMetrica + Facebook SDK. The clone does not need them to function; add only if Jean wants usage data, with its own accounts |
| AI Studio (phase 2) | Supabase Edge Functions + Storage, own AI provider keys | Server is required: image/video generation cannot run on-device. Never the original's endpoints (`api-production.8-lives-cat.io` is theirs). Provider TBD at phase 2 (e.g. Replicate / OpenAI / Gemini) |
| i18n | BM + English first, string resources ready for 60 languages | **[REA]** the original ships 60 language splits incl. Malay. Phase 1 covers BM (default) + EN, same as KebunKu |
| versioning | versionName 1.0.0, versionCode 1, applicationId `com.statushd.app` (working) | New product, new package — never `com.damtechdesigns.purepixel` |

One database (Room, local). No microservices. No server in phase 1.

## Schema

Tables: **7** (6 phase 1 + 1 phase 2). Full SQL: `replica/schema.sql`.
Access rules: local-only data, no server, no RLS — the database lives on the
device. No account, no `user_id` — the device itself is the identity (device
key in DataStore, like the original's `UserData.deviceKey`). Phase-2 AI usage
is rate-limited server-side for cost control only, never charged to the user.

| table | purpose | key fields |
| --- | --- | --- |
| `media_items` | picked gallery media | id, uri, type (video\|photo), duration_ms, width, height, size_bytes, date_added |
| `compression_jobs` | one compression run **[REA]** mirrors the original's `CompressJob` shape (own code, same function) | id, media_id FK, status (pending\|running\|delivered\|failed), preset, aspect, quality_fhd, meta_width/height/rotation, meta_hdr, clip_length_ms, duration_ms, progress, output_uri, size_before, size_after, caption, sender_number, whatsapp, seq, created_at, error |
| `split_parts` | 30 s parts of one job | id, job_id FK cascade, part_index, start_ms, end_ms, output_uri, status |
| `edit_projects` | editor state for one job (1-1) | job_id FK cascade, trim_start_ms, trim_end_ms, music_uri, music_title |
| `text_overlays` | text placed on the video | id, job_id FK cascade, text, font, size_sp, color, x, y, rotation, glow, border, shadow |
| `sticker_instances` | stickers placed on the video | id, job_id FK cascade, sticker_id, x, y, scale, rotation |
| `creations` (phase 2) | AI results gallery | id, tool, input_uri, output_uri, created_at |

Hard constraints (from recon + REA):
- No free-use meter, no Premium: removed 2026-11-14 (100% free). Every
  compression is free and unlimited. The recon open question (compressions
  vs shares) is moot.
- One running job per media item (unique index on `media_id` where
  status = 'running'); a second tap re-attaches to the running job instead of
  starting a duplicate.
- `split_parts.part_index` unique per job (no duplicate part 2 — the
  original's reported "freeze on 2nd clip" bug: re-encode each part from the
  source with its own FFmpeg invocation, no shared mutable state between parts).
- Money: prices live in Play (not in the DB). Phase 2 token purchases are
  recorded in integer cents on the server only.

## API

Phase 1 has **no server API** — everything is local. The "API" is the set of
platform integrations the app calls:

| call | does | who | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| MediaStore query | list gallery videos/photos | app (permission `READ_MEDIA_*`) | album filter | media_items | S03 |
| FFmpegKit execute | transcode to WhatsApp-target bitrate | `BackgroundCompressService` | job + preset + edit ops | output file + progress | S06 |
| FFmpegKit cancel | stop a running transcode | user (cancel button) | session id | cancelled, partial file deleted | S06, S09 |
| WhatsApp share intent (`ACTION_SEND`, package `com.whatsapp` / `com.whatsapp.w4b`) | share result to Status | user on S07 | output file URI (FileProvider) | WhatsApp opened | F01–F04 |
| WhatsApp activation (`api.whatsapp.com/send?phone=`) | one-tap activation message | user (opt-in, S07) | own WhatsApp number | WhatsApp opened with prefilled message | delivery-to-my-number |
| FileProvider + MediaStore insert | save output to gallery | app | output file | gallery URI | S07 |

Webhooks in: none (no subscriptions). Webhooks out: none.
Jobs: none in phase 1. Phase 2 adds AI job timeout cleanup (server cron).

Phase 2 server API (Supabase Edge Functions, defined now so phase 1 does not
need rework later):

| method path | does | who | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| `GET /ai/catalog` | AI feature + festival catalog (mirrors the original's `ai_catalog.json`: 22 tools, 176 festivals, token costs) | app (anonymous, device key header) | — | catalog JSON | S11 |
| `POST /ai/generate` | run one AI tool server-side | app (device key) | tool id, input file ref, params | job id | S12 |
| `GET /ai/jobs/{id}` | poll AI job status | app | job id | status, output ref when done | S12→S13 |

Only official, public APIs with Jean's own keys. Never the original app's
private endpoints, even though two are visible in the binary.

## The parts that bite

- **FFmpeg reliability (riskiest).** Transcode failures are the original's top
  complaint. Rules: one FFmpeg session per job/part; always set a timeout;
  cancel must kill the session AND delete the partial file; every failure
  shows the real error + a Retry button (never a silent dead screen); never
  hold a whole video in RAM — stream through files.
- **Tall aspect ratios (9:20 / 9:21).** The original's known bug: black bars.
  Decide once and test on a real tall video: pad to 9:16 with blurred
  background (not black bars) OR keep Original with a warning — record the
  decision in the build, do not leave it to chance.
- **30 s split.** Re-encode each part independently from the source
  (keyframe-accurate cuts); shared mutable state between parts caused the
  original's freeze on clip 2.
- **WhatsApp-target bitrate.** The whole product is "precise pre-compression
  so WhatsApp does not blur it". Presets (720p/1080p) need verified bitrates —
  recon open question; measure against real WhatsApp uploads on a test phone
  before calling presets done. Never upscale a low-quality source.
- **No revenue (100% free).** The app earns nothing; phase-2 AI generation
  is paid by Jean per call. If AI costs grow, the lever is a daily usage cap
  (cost control), never a paywall.
- **Scoped storage (SDK 29–36).** `READ_MEDIA_VIDEO/IMAGES/AUDIO`,
  `READ_MEDIA_VISUAL_USER_SELECTED` (partial access), MediaStore insert for
  saving; never assume a raw file path is writable.
- **Foreground service.** `FOREGROUND_SERVICE_MEDIA_PROCESSING` + persistent
  progress notification + `POST_NOTIFICATIONS` permission (Android 13+);
  service must survive screen-off; cancelling from the notification must
  cancel the job.
- **AI Studio (phase 2).** Usage is rate-limited server-side for cost
  control, never charged. Media uploads must be signed, short-lived URLs;
  only the chosen media leaves the device.
- **i18n.** BM default + English in phase 1; every user-visible string in
  resources from day one (60 languages later, like the original).
- **Kuv2/pk2 lessons applied.** No KSP 2.4.0 (use 2.3.13); no `kotlin-android`
  plugin; no `play-services-ads` dependency at all (no ads);
  no PNG drawables (vectors/XML); edge-to-edge + `statusBarsPadding()` /
  `navigationBarsPadding()` from the first screen; no `gradlew` in the repo
  (Android Studio builds); no WebView anywhere.

## Build order

Screens use recon S-IDs. Tables/routes per milestone as the skill requires.

1. **Vertical slice (M0).** Core loop end to end, ugly: pick video → compress
   720p → share to WhatsApp Status. Screens S01, S03, S05, S06, S07. Tables:
   `media_items`, `compression_jobs`. Integrations: MediaStore, FFmpegKit,
   foreground service, WhatsApp share intent. Proves the stack and the
   bitrate. No paywall, no ads — ever (100% free).
2. **Must-haves (M1).** Photo to HD status video (S08); splitter 30 s (S09,
   `split_parts`); save to gallery; quality presets + aspect handling (S05);
   privacy notice on first launch (S02, not a blocking gate); no paywall, no
   ads, no limits — everything free; BM + EN strings; before/after compare +
   progress + cancel + error retry (S06, S07); no-account local-first
   throughout. Tables: all 6 phase-1 tables.
3. **Should-haves (M2).** Video editor — trim, text overlays (12 fonts),
   stickers with snap-to-center, music (library / own files / extract audio)
   (S04, `edit_projects`, `text_overlays`, `sticker_instances`); AI Support
   Assistant as FAQ + email handoff first (S14); export error handling +
   retry polish.
4. **Could-haves / phase 2 (M3).** Speed Boost 4K; AI Studio hub + tools +
   My Creations (S11–S13, `creations`, server API above); no token wallet,
   no charges — free, rate-limited server-side for cost control only;
   Festival Photoshoot catalog (176 festivals, data-driven JSON like the
   original's `ai_catalog.json`); Settings language picker expansion (S15).
5. **Fixes** replica-entrepreneur finds, once it has run.

## Summary (skill contract)

- **Stack in one line:** Native Android (Kotlin + Compose, Gradle 9.8.2) +
  FFmpegKit + Room, local-first, 100% free (no billing, no ads), Supabase only
  in phase 2 for AI Studio.
- **Tables:** 7 (6 phase 1 + 1 phase 2). **Routes:** 14 screens (S10 paywall
  removed) + 3 phase-2 server endpoints.
- **Three riskiest parts:** (1) FFmpeg transcode reliability + export
  failures; (2) WhatsApp-target bitrate presets (unverified — must be
  measured on a real phone); (3) no revenue — phase-2 AI costs are Jean's cost.
- **Next step:** `/replica-design` (visual system + screen layouts), then
  `/replica-build`.
