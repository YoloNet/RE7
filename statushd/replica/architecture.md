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
| billing | Google Play Billing Library (direct, no RevenueCat) | Simpler than the original's RevenueCat stack; no third-party dependency. Products: Weekly / Monthly / Yearly subs + Compress Pass |
| ads | AdMob (own account), banner + interstitial | **[recon]** features.csv must-have. Own AdMob account + real App ID — never a fake ID (a missing App ID crashes at launch; kuv2 lesson) |
| consent | Google UMP / Funding Choices (IAB TCF) | **[REA]** the original shows a first-launch consent form (UMP strings + `fundingchoicesmessages.google.com`). Required for ads in EEA/UK |
| anti-piracy | Play Integrity API / Play App Licensing | The original uses PairIP (proprietary, paid) — do NOT copy; Play's own tools instead |
| analytics | None in phase 1 (optional: Firebase Analytics later, own project) | The original ships Firebase + AppMetrica + Facebook SDK. The clone does not need them to function; add only if Jean wants usage data, with its own accounts |
| AI Studio (phase 2) | Supabase Edge Functions + Storage, own AI provider keys | Server is required: image/video generation cannot run on-device. Never the original's endpoints (`api-production.8-lives-cat.io` is theirs). Provider TBD at phase 2 (e.g. Replicate / OpenAI / Gemini) |
| i18n | BM + English first, string resources ready for 60 languages | **[REA]** the original ships 60 language splits incl. Malay. Phase 1 covers BM (default) + EN, same as KebunKu |
| versioning | versionName 1.0.0, versionCode 1, applicationId `com.statushd.app` (working) | New product, new package — never `com.damtechdesigns.purepixel` |

One database (Room, local). No microservices. No server in phase 1.

## Schema

Tables: **9** (7 phase 1 + 2 phase 2). Full SQL: `replica/schema.sql`.
Access rules: local-only data, no server, no RLS — the database lives on the
device; entitlement is verified against Play Billing, AI credits (phase 2) are
verified server-side. No account, no `user_id` — the device itself is the
identity (device key in DataStore, like the original's `UserData.deviceKey`).

| table | purpose | key fields |
| --- | --- | --- |
| `media_items` | picked gallery media | id, uri, type (video\|photo), duration_ms, width, height, size_bytes, date_added |
| `compression_jobs` | one compression run **[REA]** mirrors the original's `CompressJob` shape (own code, same function) | id, media_id FK, status (pending\|running\|delivered\|failed), preset, aspect, quality_fhd, meta_width/height/rotation, meta_hdr, clip_length_ms, duration_ms, progress, output_uri, size_before, size_after, premium, caption, sender_number, whatsapp, seq, created_at, error |
| `split_parts` | 30 s parts of one job | id, job_id FK cascade, part_index, start_ms, end_ms, output_uri, status |
| `edit_projects` | editor state for one job (1-1) | job_id FK cascade, trim_start_ms, trim_end_ms, music_uri, music_title |
| `text_overlays` | text placed on the video | id, job_id FK cascade, text, font, size_sp, color, x, y, rotation, glow, border, shadow |
| `sticker_instances` | stickers placed on the video | id, job_id FK cascade, sticker_id, x, y, scale, rotation |
| `entitlement` | single row (id = 1): Premium state + free-use meter | premium, plan, expiry_at, compress_pass, free_uses_today, free_uses_date |
| `ai_credits` (phase 2) | local mirror of the token wallet | balance, monthly_grant_date, last_sync_at |
| `creations` (phase 2) | AI results gallery | id, tool, input_uri, output_uri, created_at |

Hard constraints (from recon + REA):
- Free-use meter: **3 completed compressions/day** (status `delivered` counts).
  Increment and check inside one Room transaction — a double-tap must not
  count twice. Reset when `free_uses_date` != today (local date).
  Open question from recon (compressions vs shares): decided as compressions
  for v1; revisit if reviews/user testing say otherwise.
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
| Play Billing `queryProductDetails` / `launchBillingFlow` | show plans, start purchase | user on S10 | product ids | purchase token | F05 |
| Play Billing `acknowledgePurchase` + `queryPurchases` | verify + restore | app on start / S10 restore | purchase token | entitlement row updated | F05, app start |
| AdMob load/show | banner (S01) + interstitial (after S06) | app (only after UMP consent) | ad unit id | ad shown / not loaded | S01, S06→S07 |
| UMP `requestConsentInfoUpdate` + `showPrivacyOptionsForm` | first-launch consent gate | app on first launch | — | consent status | S02 |
| WhatsApp share intent (`ACTION_SEND`, package `com.whatsapp` / `com.whatsapp.w4b`) | share result to Status | user on S07 | output file URI (FileProvider) | WhatsApp opened | F01–F04 |
| WhatsApp activation (`api.whatsapp.com/send?phone=`) | one-tap activation message | user (opt-in, S07) | own WhatsApp number | WhatsApp opened with prefilled message | delivery-to-my-number |
| FileProvider + MediaStore insert | save output to gallery | app | output file | gallery URI | S07 |

Webhooks in (phase 1): none. Phase 2 adds Play RTDN (subscription state
changes) → Supabase Edge Function → entitlement mirror.
Webhooks out: none.
Jobs: free-use reset is local (checked on app start + at midnight via
WorkManager, no server). Phase 2 adds: AI job timeout cleanup (server cron),
monthly token grant (server cron).

Phase 2 server API (Supabase Edge Functions, defined now so phase 1 does not
need rework later):

| method path | does | who | input | output | flow |
| --- | --- | --- | --- | --- | --- |
| `GET /ai/catalog` | AI feature + festival catalog (mirrors the original's `ai_catalog.json`: 22 tools, 176 festivals, token costs) | app (anonymous, device key header) | — | catalog JSON | S11 |
| `POST /ai/generate` | run one AI tool server-side | app (device key + enough tokens) | tool id, input file ref, params | job id, tokens charged | S12 |
| `GET /ai/jobs/{id}` | poll AI job status | app | job id | status, output ref when done | S12→S13 |
| `POST /ai/tokens/verify` | verify a Play token-pack purchase, credit wallet | app (purchase token) | product id, purchase token | new balance | S11/S12 buy tokens |
| `GET /ai/credits` | current token balance | app (device key) | — | balance, next grant date | S11 |

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
- **Free-limit metering races.** Double-tap / process death mid-compression
  must not double-count or lose a use: single Room transaction, status
  machine `pending → running → delivered|failed`, counter increments only on
  `delivered`.
- **Billing.** Restore purchases on every fresh install (no account to recover
  from); handle pending purchases, grace period, and expired subs; test with
  Play license testers before release.
- **Scoped storage (SDK 29–36).** `READ_MEDIA_VIDEO/IMAGES/AUDIO`,
  `READ_MEDIA_VISUAL_USER_SELECTED` (partial access), MediaStore insert for
  saving; never assume a raw file path is writable.
- **Foreground service.** `FOREGROUND_SERVICE_MEDIA_PROCESSING` + persistent
  progress notification + `POST_NOTIFICATIONS` permission (Android 13+);
  service must survive screen-off; cancelling from the notification must
  cancel the job.
- **Ads + consent ordering.** UMP consent must resolve BEFORE any ad request;
  ads never load on the paywall or consent screens.
- **AI Studio (phase 2).** Token balance is authoritative on the server, not
  in the app — otherwise it can be tampered with. Media uploads must be
  signed, short-lived URLs; only the chosen media leaves the device.
- **i18n.** BM default + English in phase 1; every user-visible string in
  resources from day one (60 languages later, like the original).
- **Kuv2/pk2 lessons applied.** No KSP 2.4.0 (use 2.3.13); no `kotlin-android`
  plugin; no `play-services-ads` dependency until a real AdMob App ID exists;
  no PNG drawables (vectors/XML); edge-to-edge + `statusBarsPadding()` /
  `navigationBarsPadding()` from the first screen; no `gradlew` in the repo
  (Android Studio builds); no WebView anywhere.

## Build order

Screens use recon S-IDs. Tables/routes per milestone as the skill requires.

1. **Vertical slice (M0).** Core loop end to end, ugly: pick video → compress
   720p → share to WhatsApp Status. Screens S01, S03, S05, S06, S07. Tables:
   `media_items`, `compression_jobs`. Integrations: MediaStore, FFmpegKit,
   foreground service, WhatsApp share intent. Proves the stack and the
   bitrate. No paywall, no ads yet.
2. **Must-haves (M1).** Photo to HD status video (S08); splitter 30 s (S09,
   `split_parts`); save to gallery; quality presets + aspect handling (S05);
   free-use limit 3/day + paywall + restore (S10, `entitlement`); privacy
   consent gate (S02); ads — banner S01 + interstitial after S06 (own AdMob);
   BM + EN strings; before/after compare + progress + cancel + error retry
   (S06, S07); no-account local-first throughout. Tables: all 7 phase-1
   tables.
3. **Should-haves (M2).** Video editor — trim, text overlays (12 fonts),
   stickers with snap-to-center, music (library / own files / extract audio)
   (S04, `edit_projects`, `text_overlays`, `sticker_instances`); AI Support
   Assistant as FAQ + email handoff first (S14); export error handling +
   retry polish.
4. **Could-haves / phase 2 (M3).** Speed Boost 4K; AI Studio hub + tools +
   token wallet + My Creations (S11–S13, `ai_credits`, `creations`, server
   API above); Festival Photoshoot catalog (176 festivals, data-driven JSON
   like the original's `ai_catalog.json`); Settings language picker expansion
   (S15).
5. **Fixes** replica-entrepreneur finds, once it has run.

## Summary (skill contract)

- **Stack in one line:** Native Android (Kotlin + Compose, Gradle 9.8.2) +
  FFmpegKit + Room, local-first, Play Billing + AdMob, Supabase only in
  phase 2 for AI Studio.
- **Tables:** 9 (7 phase 1 + 2 phase 2). **Routes:** 15 screens + 5 phase-2
  server endpoints.
- **Three riskiest parts:** (1) FFmpeg transcode reliability + export
  failures; (2) WhatsApp-target bitrate presets (unverified — must be
  measured on a real phone); (3) free-limit metering + billing restore
  without an account.
- **Next step:** `/replica-design` (visual system + screen layouts), then
  `/replica-build`.
